#!/usr/bin/env python3
import requests
from bs4 import BeautifulSoup
from urllib.parse import urljoin, urlparse
import json
from time import sleep
from collections import defaultdict
import concurrent.futures
import psycopg2
from psycopg2 import sql
from datetime import datetime
import gzip
import os
from dotenv import load_dotenv
import hashlib
import argparse
import sys

# Load environment variables
load_dotenv()

class SpringIntegratedScraper:
    def __init__(self, base_url=None, spring_data_mode=False):
        self.base_url = base_url.rstrip('/') if base_url else os.getenv('BASE_URL', 'https://www.ppcbank.com.kh/').rstrip('/')
        self.domain = urlparse(self.base_url).netloc
        self.spring_data_mode = spring_data_mode
        self.session = requests.Session()
        self.session.headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
            'Accept-Language': 'en-US,en;q=0.5',
            'Accept-Encoding': 'gzip, deflate'
        }
        self.valid_pages = []
        self.broken_links = []
        self.crawl_delay = float(os.getenv('CRAWL_DELAY', '0.5'))
        self.max_depth = int(os.getenv('MAX_DEPTH', '3'))
        self.visited = set()
        self.max_content_length = int(os.getenv('MAX_CONTENT_LENGTH', '5000'))
        self.max_workers = int(os.getenv('MAX_WORKERS', '5'))
        self._init_db()

    def _init_db(self):
        """Initialize PostgreSQL database connection"""
        try:
            self.conn = psycopg2.connect(
                # dbname=os.getenv('DB_NAME', 'postgres'),
                # user=os.getenv('DB_USER', 'postgres'),
                # password=os.getenv('DB_PASSWORD', 'bizwebadmin123$'),
                # host=os.getenv('DB_HOST', '192.168.178.239'),
                # port=os.getenv('DB_PORT', '5432')
                dbname=os.getenv('DB_NAME', 'web_scraper'),
                user=os.getenv('DB_USER', 'postgres'),
                password=os.getenv('DB_PASSWORD', '12345678'),
                host=os.getenv('DB_HOST', 'localhost'),
                port=os.getenv('DB_PORT', '5433')
            )
            print("Successfully connected to PostgreSQL database")
        except psycopg2.Error as e:
            print(f"PostgreSQL connection error: {e}")
            raise

    def is_valid_url(self, url):
        """Check if URL is valid and belongs to the target domain"""
        try:
            parsed = urlparse(url)
            return (parsed.netloc == self.domain and
                    parsed.scheme in ['http', 'https'] and
                    not any(ext in url.lower() for ext in ['.pdf', '.jpg', '.png', '.gif', '.css', '.js']))
        except:
            return False

    def _calculate_content_hash(self, content):
        """Calculate MD5 hash of content for change detection"""
        return hashlib.md5(content.encode('utf-8')).hexdigest()

    def _store_page_version(self, page_id, page_data):
        """Store version history"""
        try:
            with self.conn.cursor() as cursor:
                cursor.execute("""
                               INSERT INTO page_versions (
                                   page_id, url, title, content, content_hash, status, depth, error
                               ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                               """, (
                                   page_id,
                                   page_data['url'],
                                   page_data['title'],
                                   page_data['content'],
                                   page_data['content_hash'],
                                   page_data['status'],
                                   page_data['depth'],
                                   page_data.get('error')
                               ))
                self.conn.commit()
                print(f"Stored version history for page ID: {page_id}")
        except psycopg2.Error as e:
            print(f"Error storing page version: {e}")
            self.conn.rollback()

    def store_page(self, page_data):
        """Store page data including table content in tb_ppc_bank table"""
        try:
            with self.conn.cursor() as cursor:
                # Check if page exists
                cursor.execute(
                    "SELECT id, content_hash FROM tb_ppc_bank WHERE url = %s",
                    (page_data['url'],)
                )
                existing_page = cursor.fetchone()

                # Prepare common parameters
                params = {
                    'url': page_data['url'],
                    'title': page_data['title'],
                    'content': page_data['content'],
                    'content_hash': page_data['content_hash'],
                    'status': page_data['status'],
                    'depth': page_data['depth'],
                    'error': page_data.get('error'),
                    'tables': page_data.get('tables', json.dumps([]))  # Default empty array if not provided
                }

                if existing_page:
                    page_id, current_hash = existing_page
                    if current_hash == params['content_hash'] and not params['error']:
                        # Update timestamp only if content hasn't changed
                        cursor.execute("""
                                       UPDATE tb_ppc_bank
                                       SET updated_at = CURRENT_TIMESTAMP
                                       WHERE id = %s
                                       """, (page_id,))
                        self.conn.commit()
                        print(f"Page timestamp updated: {params['url']}")
                        return False
                    else:
                        # Update all fields including tables
                        cursor.execute("""
                                       UPDATE tb_ppc_bank
                                       SET title = %(title)s,
                                           content = %(content)s,
                                           content_hash = %(content_hash)s,
                                           status = %(status)s,
                                           depth = %(depth)s,
                                           error = %(error)s,
                                           tables = %(tables)s::jsonb,
                                updated_at = CURRENT_TIMESTAMP
                                       WHERE id = %s
                                       """, {**params, 'id': page_id})
                        self.conn.commit()
                        print(f"Page updated: {params['url']}")
                        return True
                else:
                    # Insert new page with all fields including tables
                    cursor.execute("""
                                   INSERT INTO tb_ppc_bank (
                                       url, title, content, content_hash,
                                       status, depth, error, tables
                                   ) VALUES (
                                                %(url)s, %(title)s, %(content)s, %(content_hash)s,
                                                %(status)s, %(depth)s, %(error)s, %(tables)s::jsonb
                                            )
                                       RETURNING id
                                   """, params)
                    page_id = cursor.fetchone()[0]
                    self.conn.commit()
                    print(f"New page stored: {params['url']} (ID: {page_id})")
                    return True

        except psycopg2.Error as e:
            print(f"Database error storing page {page_data['url']}: {e}")
            self.conn.rollback()
            return False
        except Exception as e:
            print(f"Unexpected error storing page {page_data['url']}: {e}")
            self.conn.rollback()
            return False
    def extract_table_data(self, soup):
        """Extract and structure data from HTML tables with improved logic"""
        tables_data = []

        for table in soup.find_all('table'):
            table_data = {
                'caption': table.find('caption').get_text(strip=True) if table.find('caption') else None,
                'headers': [],
                'rows': [],
                'table_attributes': dict(table.attrs)
            }

            # Extract headers - look for th in thead first, then fall back to th in table
            header_row = table.find('thead')
            if header_row:
                headers = [th.get_text(strip=True) for th in header_row.find_all('th')]
            else:
                headers = [th.get_text(strip=True) for th in table.find_all('th', recursive=False)]

            table_data['headers'] = headers if headers else []

            # Extract table body rows
            tbody = table.find('tbody') or table  # Fall back to table if no tbody
            for row in tbody.find_all('tr', recursive=False):  # Only direct children
                cells = []
                for cell in row.find_all(['td', 'th'], recursive=False):  # Only direct children
                    cell_text = cell.get_text(' ', strip=True)  # Preserve some whitespace
                    cell_data = {
                        'text': cell_text,
                        'attributes': dict(cell.attrs),
                        'colspan': int(cell.get('colspan', 1)),
                        'rowspan': int(cell.get('rowspan', 1))
                    }
                    cells.append(cell_data)

                if cells:  # Only add rows with cells
                    row_data = {
                        'cells': cells,
                        'row_attributes': dict(row.attrs)
                    }
                    table_data['rows'].append(row_data)

            tables_data.append(table_data)

        return tables_data

    def scrape_page(self, url, depth=0):
        """Scrape individual page with proper table handling and error management"""
        if depth > self.max_depth or url in self.visited:
            return

        self.visited.add(url)
        sleep(self.crawl_delay)

        # Initialize page data with default values
        page_data = {
            'url': url,
            'title': None,
            'content': None,
            'content_hash': None,
            'status': None,
            'depth': depth,
            'error': None,
            'tables': None
        }

        try:
            response = self.session.get(url, timeout=10)
            page_data['status'] = response.status_code
            response.raise_for_status()

            if 'text/html' not in response.headers.get('Content-Type', ''):
                raise ValueError(f"Non-HTML content (Content-Type: {response.headers.get('Content-Type')})")

            soup = BeautifulSoup(response.text, 'html.parser')

            # Remove unwanted elements (keeping tables)
            for selector in ['script', 'style', 'iframe', 'nav', 'footer',
                             '[class*="cookie"]', '[id*="cookie"]']:
                for element in soup.select(selector):
                    element.decompose()

            # Extract all text content (including from tables)
            all_text = ' '.join([text for text in soup.stripped_strings])
            if len(all_text) > self.max_content_length:
                all_text = all_text[:self.max_content_length] + '... [CONTENT TRUNCATED]'

            # Extract structured table data
            tables_data = self.extract_table_data(soup)

            # Create comprehensive page data
            structured_content = {
                'text_content': all_text,
                'tables': tables_data
            }

            page_data.update({
                'title': soup.title.string.strip() if soup.title and soup.title.string else 'No Title',
                'content': json.dumps(structured_content),
                'content_hash': self._calculate_content_hash(json.dumps(structured_content)),
                'tables': json.dumps(tables_data)
            })

            # Store the page
            self.store_page(page_data)
            self.valid_pages.append(page_data)

            # Extract and follow links (only on successful scrape)
            if depth < self.max_depth:
                links = set()
                for link in soup.find_all('a', href=True):
                    href = link['href']
                    if not href or href.startswith(('javascript:', 'mailto:', 'tel:')):
                        continue

                    new_url = urljoin(self.base_url, href).split('#')[0].split('?')[0].rstrip('/')
                    if self.is_valid_url(new_url) and new_url not in self.visited:
                        links.add(new_url)

                # Process links concurrently if any were found
                if links:
                    with concurrent.futures.ThreadPoolExecutor(max_workers=self.max_workers) as executor:
                        futures = [executor.submit(self.scrape_page, link, depth + 1) for link in links]
                        concurrent.futures.wait(futures)

        except Exception as e:
            error_msg = f"{type(e).__name__}: {str(e)}"
            page_data.update({
                'content': error_msg,
                'content_hash': self._calculate_content_hash(error_msg),
                'error': error_msg,
                'tables': json.dumps([])  # Empty tables array for error case
            })
            if page_data['status'] is None:
                page_data['status'] = 500  # Default error status

            self.broken_links.append(page_data)
            self.store_page(page_data)

    def crawl_site(self, force_update=False):
        """Start crawling with option to force update all pages"""
        print(f"Starting crawl of {self.base_url}")
        start_time = datetime.now()

        if force_update:
            print("Forced update mode - will rescrape all pages")
            self.visited = set()  # Clear visited cache

        self.scrape_page(self.base_url)

        end_time = datetime.now()
        duration = (end_time - start_time).total_seconds()
        print(f"Crawl completed in {duration:.2f} seconds")

        return {
            'valid_pages': len(self.valid_pages),
            'broken_links': len(self.broken_links),
            'duration_seconds': duration
        }

    def close(self):
        """Clean up resources"""
        try:
            if hasattr(self, 'conn') and self.conn:
                self.conn.close()
            self.session.close()
            print("Resources cleaned up successfully")
        except Exception as e:
            print(f"Error during cleanup: {e}")

def main():
    parser = argparse.ArgumentParser(description='Website scraper integrated with Spring Boot')
    parser.add_argument('--spring-data-mode', action='store_true',
                        help='Run in Spring Data integration mode')
    parser.add_argument('--url', type=str,
                        help='Base URL to scrape (overrides environment variable)')
    parser.add_argument('--force-update', action='store_true',
                        help='Force update all pages regardless of changes')
    args = parser.parse_args()

    try:
        scraper = SpringIntegratedScraper(
            base_url=args.url,
            spring_data_mode=args.spring_data_mode
        )

        result = scraper.crawl_site(force_update=args.force_update)
        print("\n=== Scraping Summary ===")
        print(f"Pages processed: {result['valid_pages']}")
        print(f"Broken links found: {result['broken_links']}")
        print(f"Time taken: {result['duration_seconds']:.2f} seconds")

        if result['valid_pages'] == 0:
            print("No valid pages were processed. Check the base URL and network connectivity.")
            sys.exit(1)
        else:
            sys.exit(0)

    except Exception as e:
        print(f"Fatal error during scraping: {e}")
        sys.exit(1)
    finally:
        if 'scraper' in locals():
            scraper.close()

if __name__ == "__main__":
    main()