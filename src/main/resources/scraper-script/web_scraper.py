
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
                dbname=os.getenv('DB_NAME', 'web_scraper'),
                user=os.getenv('DB_USER', 'mac_pg'),
                password=os.getenv('DB_PASSWORD', '12345678'),
                host=os.getenv('DB_HOST', 'localhost'),
                port=os.getenv('DB_PORT', '5432')
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

    def _store_broken_link(self, url, error):
        """Store broken link information in database"""
        try:
            with self.conn.cursor() as cursor:
                cursor.execute("""
                               INSERT INTO broken_links (url, error)
                               VALUES (%s, %s)
                               """, (url, error))
                self.conn.commit()
                print(f"Broken link stored: {url} - {error}")
                self.broken_links.append({'url': url, 'error': error})
        except psycopg2.Error as e:
            print(f"Error storing broken link: {e}")
            self.conn.rollback()

    def _calculate_content_hash(self, content):
        """Calculate MD5 hash of content for change detection"""
        return hashlib.md5(content.encode('utf-8')).hexdigest()

    def _store_page_version(self, page_id, page_data):
        """Store version history"""
        try:
            with self.conn.cursor() as cursor:
                cursor.execute("""
                               INSERT INTO page_versions (
                                   page_id, url, title, content, content_hash, status, depth
                               ) VALUES (%s, %s, %s, %s, %s, %s, %s)
                               """, (
                                   page_id,
                                   page_data['url'],
                                   page_data['title'],
                                   page_data['content'],
                                   page_data['content_hash'],
                                   page_data['status'],
                                   page_data['depth']
                               ))
                self.conn.commit()
                print(f"Stored version history for page ID: {page_id}")
        except psycopg2.Error as e:
            print(f"Error storing page version: {e}")
            self.conn.rollback()

    def store_page(self, page_data):
        """Store page data in format compatible with Spring Data JPA"""
        try:
            with self.conn.cursor() as cursor:
                # Check if page exists
                cursor.execute("SELECT id, content_hash FROM pages WHERE url = %s", (page_data['url'],))
                existing_page = cursor.fetchone()

                if existing_page:
                    page_id, current_hash = existing_page
                    if current_hash == page_data['content_hash']:
                        # Update timestamp only
                        cursor.execute("""
                                       UPDATE pages
                                       SET updated_at = CURRENT_TIMESTAMP
                                       WHERE id = %s
                                       """, (page_id,))
                        self.conn.commit()
                        print(f"Page timestamp updated: {page_data['url']}")
                        return False
                    else:
                        # Get current version before updating
                        cursor.execute("""
                                       SELECT url, title, content, content_hash, status, depth
                                       FROM pages
                                       WHERE id = %s
                                       """, (page_id,))
                        old_version = cursor.fetchone()

                        # Store old version
                        self._store_page_version(page_id, {
                            'url': old_version[0],
                            'title': old_version[1],
                            'content': old_version[2],
                            'content_hash': old_version[3],
                            'status': old_version[4],
                            'depth': old_version[5]
                        })

                        # Update current page
                        cursor.execute("""
                                       UPDATE pages
                                       SET title = %s,
                                           content = %s,
                                           content_hash = %s,
                                           status = %s,
                                           depth = %s,
                                           updated_at = CURRENT_TIMESTAMP
                                       WHERE id = %s
                                       """, (
                                           page_data['title'],
                                           page_data['content'],
                                           page_data['content_hash'],
                                           page_data['status'],
                                           page_data['depth'],
                                           page_id
                                       ))
                        self.conn.commit()
                        print(f"Page updated: {page_data['url']}")
                        return True
                else:
                    # Insert new page
                    cursor.execute("""
                                   INSERT INTO pages (
                                       url, title, content, content_hash, status, depth
                                   ) VALUES (%s, %s, %s, %s, %s, %s)
                                       RETURNING id
                                   """, (
                                       page_data['url'],
                                       page_data['title'],
                                       page_data['content'],
                                       page_data['content_hash'],
                                       page_data['status'],
                                       page_data['depth']
                                   ))
                    page_id = cursor.fetchone()[0]
                    self.conn.commit()
                    print(f"New page stored: {page_data['url']} (ID: {page_id})")
                    return True
        except psycopg2.Error as e:
            print(f"Error storing page: {e}")
            self.conn.rollback()
            return False

    def scrape_page(self, url, depth=0):
        """Scrape individual page with change detection"""
        if depth > self.max_depth or url in self.visited:
            return

        self.visited.add(url)
        sleep(self.crawl_delay)

        try:
            response = self.session.get(url, timeout=10)
            response.raise_for_status()

            if 'text/html' not in response.headers.get('Content-Type', ''):
                self._store_broken_link(url, 'Non-HTML content')
                return

            soup = BeautifulSoup(response.text, 'html.parser')

            # Remove unwanted elements
            for selector in ['script', 'style', 'iframe', 'nav', 'footer',
                             '[class*="cookie"]', '[id*="cookie"]']:
                for element in soup.select(selector):
                    element.decompose()

            # Get content
            content = ' '.join([text for text in soup.stripped_strings if len(text.split()) > 1])
            if len(content) > self.max_content_length:
                content = content[:self.max_content_length] + '... [CONTENT TRUNCATED]'

            content_hash = self._calculate_content_hash(content)

            page_data = {
                'url': url,
                'title': soup.title.string if soup.title else None,
                'content': content,
                'content_hash': content_hash,
                'status': response.status_code,
                'depth': depth
            }

            # Store page
            was_updated = self.store_page(page_data)
            self.valid_pages.append(page_data)

            # Extract and follow links
            if depth < self.max_depth:
                links = set()
                for link in soup.find_all('a', href=True):
                    href = link['href']
                    if not href or href.startswith(('javascript:', 'mailto:', 'tel:')):
                        continue

                    new_url = urljoin(self.base_url, href).split('#')[0].split('?')[0].rstrip('/')
                    if self.is_valid_url(new_url) and new_url not in self.visited:
                        links.add(new_url)

                # Process links concurrently
                with concurrent.futures.ThreadPoolExecutor(max_workers=self.max_workers) as executor:
                    futures = [executor.submit(self.scrape_page, link, depth + 1) for link in links]
                    concurrent.futures.wait(futures)

        except requests.exceptions.RequestException as e:
            self._store_broken_link(url, f"Request failed: {str(e)}")
        except Exception as e:
            self._store_broken_link(url, f"Scraping failed: {str(e)}")

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