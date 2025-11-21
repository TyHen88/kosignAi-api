#!/usr/bin/env python3
import requests
from bs4 import BeautifulSoup
from urllib.parse import urljoin, urlparse
import json
from time import sleep
import concurrent.futures
import psycopg2
from datetime import datetime
import os
from dotenv import load_dotenv
import hashlib
import argparse
import sys

# Load environment variables
load_dotenv()


class SpringIntegratedScraper:
    def __init__(self, base_url=None, spring_data_mode=False):
        self.base_url = (
            base_url.rstrip("/")
            if base_url
            else os.getenv("BASE_URL", "https://www.ppcbank.com.kh/").rstrip("/")
        )
        self.domain = urlparse(self.base_url).netloc
        self.spring_data_mode = spring_data_mode
        self.session = requests.Session()
        self.session.headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
            "Accept-Language": "en-US,en;q=0.5",
            "Accept-Encoding": "gzip, deflate",
        }
        self.valid_pages = []
        self.broken_links = []
        self.crawl_delay = float(os.getenv("CRAWL_DELAY", "0.5"))
        self.max_depth = int(os.getenv("MAX_DEPTH", "3"))
        self.visited = set()
        self.max_content_length = int(os.getenv("MAX_CONTENT_LENGTH", "5000"))
        self.max_workers = int(os.getenv("MAX_WORKERS", "5"))
        self._init_db()

    def _init_db(self):
        try:
            self.conn = psycopg2.connect(
                dbname=os.getenv("DB_NAME", "web_scraper"),
                user=os.getenv("DB_USER", "mac_pg"),
                password=os.getenv("DB_PASSWORD", "12345678"),
                host=os.getenv("DB_HOST", "localhost"),
                port=os.getenv("DB_PORT", "5432"),
            )
            print("Successfully connected to PostgreSQL database")
        except psycopg2.Error as e:
            print(f"PostgreSQL connection error: {e}")
            raise

    def is_valid_url(self, url):
        try:
            parsed = urlparse(url)
            return (
                parsed.netloc == self.domain
                and parsed.scheme in ["http", "https"]
                and not any(
                    ext in url.lower()
                    for ext in [".pdf", ".jpg", ".png", ".gif", ".css", ".js"]
                )
            )
        except:
            return False

    def _calculate_content_hash(self, content):
        return hashlib.md5(content.encode("utf-8")).hexdigest()

    def extract_sections_and_content(self, soup):
        sections = []
        current_section = None
        body = soup.body
        if not body:
            return sections
        for elem in body.descendants:
            if getattr(elem, "name", None) in ["h1", "h2", "h3", "h4", "h5", "h6"]:
                if current_section:
                    sections.append(current_section)
                current_section = {
                    "heading": elem.get_text(strip=True),
                    "paragraphs": [],
                    "list": [],
                }
            elif elem.name == "p" and current_section:
                text = elem.get_text(strip=True)
                if text:
                    current_section["paragraphs"].append(text)
            elif elem.name in ["ul", "ol"] and current_section:
                items = [li.get_text(strip=True) for li in elem.find_all("li")]
                current_section["list"].extend(items)
        if current_section:
            sections.append(current_section)
        return sections

    def extract_tables_for_content_json(self, soup):
        tables = []
        for table in soup.find_all("table"):
            headers = [th.get_text(strip=True) for th in table.find_all("th")]
            rows = []
            for tr in table.find_all("tr"):
                cells = [td.get_text(strip=True) for td in tr.find_all(["td", "th"])]
                if cells:
                    rows.append(cells)
            tables.append(
                {
                    "title": (
                        table.find("caption").get_text(strip=True)
                        if table.find("caption")
                        else None
                    ),
                    "headers": headers,
                    "rows": rows,
                }
            )
        return tables

    def store_page(self, page_data):
        try:
            with self.conn.cursor() as cursor:
                cursor.execute(
                    "SELECT id, content_hash FROM tb_ppc_bank WHERE url = %s",
                    (page_data["url"],),
                )
                existing_page = cursor.fetchone()
                params = {
                    "url": page_data["url"],
                    "title": page_data["title"],
                    "content": page_data["content"],
                    "content_hash": page_data["content_hash"],
                    "status": page_data["status"],
                    "depth": page_data["depth"],
                    "error": page_data.get("error"),
                    "content_json": page_data.get("content_json", json.dumps({})),
                }
                if existing_page:
                    page_id, current_hash = existing_page
                    if current_hash == params["content_hash"] and not params["error"]:
                        cursor.execute(
                            """
                            UPDATE tb_ppc_bank SET updated_at = CURRENT_TIMESTAMP WHERE id = %s
                        """,
                            (page_id,),
                        )
                        self.conn.commit()
                        print(f"Page timestamp updated: {params['url']}")
                        return False
                    else:
                        cursor.execute(
                            """
                            UPDATE tb_ppc_bank SET title = %(title)s, content = %(content)s,
                            content_hash = %(content_hash)s, status = %(status)s, depth = %(depth)s,
                            error = %(error)s, content_json = %(content_json)s::jsonb, updated_at = CURRENT_TIMESTAMP
                            WHERE id = %(id)s
                        """,
                            {**params, "id": page_id},
                        )
                        self.conn.commit()
                        print(f"Page updated: {params['url']}")
                        return True
                else:
                    cursor.execute(
                        """
                        INSERT INTO tb_ppc_bank (url, title, content, content_hash, status,
                        depth, error, content_json) VALUES (%(url)s, %(title)s, %(content)s,
                        %(content_hash)s, %(status)s, %(depth)s, %(error)s, %(content_json)s::jsonb)
                        RETURNING id
                    """,
                        params,
                    )
                    result = cursor.fetchone()
                    if result:
                        self.conn.commit()
                        print(f"New page stored: {params['url']} (ID: {result[0]})")
                        return True
                    else:
                        print(f"Failed to insert page: {params['url']}")
                        return False
        except Exception as e:
            print(f"Error storing page {page_data['url']}: {e}")
            self.conn.rollback()
            return False

    def scrape_page(self, url, depth=0):
        if depth > self.max_depth or url in self.visited:
            return
        self.visited.add(url)
        sleep(self.crawl_delay)
        page_data = {
            "url": url,
            "title": None,
            "content": None,
            "content_hash": None,
            "status": None,
            "depth": depth,
            "error": None,
            "content_json": None,
        }
        try:
            response = self.session.get(url, timeout=10)
            page_data["status"] = response.status_code
            response.raise_for_status()
            if "text/html" not in response.headers.get("Content-Type", ""):
                raise ValueError("Non-HTML content")
            soup = BeautifulSoup(response.text, "html.parser")
            for selector in [
                "script",
                "style",
                "iframe",
                "nav",
                "footer",
                '[class*="cookie"]',
                '[id*="cookie"]',
            ]:
                for element in soup.select(selector):
                    element.decompose()
            sections = self.extract_sections_and_content(soup)
            tables = self.extract_tables_for_content_json(soup)
            content_json_obj = {
                "title": (
                    soup.title.string.strip()
                    if soup.title and soup.title.string
                    else "No Title"
                ),
                "sections": sections,
                "tables": tables,
            }
            content_json_str = json.dumps(content_json_obj, ensure_ascii=False)
            page_data.update(
                {
                    "title": content_json_obj["title"],
                    "content": content_json_str,
                    "content_hash": self._calculate_content_hash(content_json_str),
                    "content_json": content_json_str,
                }
            )
            self.store_page(page_data)
            self.valid_pages.append(page_data)
            if depth < self.max_depth:
                links = set()
                for link in soup.find_all("a", href=True):
                    href = (
                        str(link.attrs.get("href", ""))
                        .split("#")[0]
                        .split("?")[0]
                        .rstrip("/")
                    )
                    new_url = urljoin(self.base_url, href)
                    if self.is_valid_url(new_url) and new_url not in self.visited:
                        links.add(new_url)
                with concurrent.futures.ThreadPoolExecutor(
                    max_workers=self.max_workers
                ) as executor:
                    futures = [
                        executor.submit(self.scrape_page, link, depth + 1)
                        for link in links
                    ]
                    concurrent.futures.wait(futures)
        except Exception as e:
            error_msg = f"{type(e).__name__}: {str(e)}"
            page_data.update(
                {
                    "content": error_msg,
                    "content_hash": self._calculate_content_hash(error_msg),
                    "error": error_msg,
                    "content_json": json.dumps({}),
                }
            )
            if page_data["status"] is None:
                page_data["status"] = 500
            self.broken_links.append(page_data)
            self.store_page(page_data)

    def crawl_site(self, force_update=False):
        print(f"Starting crawl of {self.base_url}")
        start_time = datetime.now()
        if force_update:
            self.visited = set()
        self.scrape_page(self.base_url)
        end_time = datetime.now()
        duration = (end_time - start_time).total_seconds()
        print(f"Crawl completed in {duration:.2f} seconds")
        return {
            "valid_pages": len(self.valid_pages),
            "broken_links": len(self.broken_links),
            "duration_seconds": duration,
        }

    def close(self):
        try:
            if hasattr(self, "conn") and self.conn:
                self.conn.close()
            self.session.close()
            print("Resources cleaned up successfully")
        except Exception as e:
            print(f"Error during cleanup: {e}")


def main():
    parser = argparse.ArgumentParser(
        description="Website scraper integrated with Spring Boot"
    )
    parser.add_argument(
        "--spring-data-mode",
        action="store_true",
        help="Run in Spring Data integration mode",
    )
    parser.add_argument(
        "--url", type=str, help="Base URL to scrape (overrides environment variable)"
    )
    parser.add_argument(
        "--force-update",
        action="store_true",
        help="Force update all pages regardless of changes",
    )
    args = parser.parse_args()
    try:
        scraper = SpringIntegratedScraper(
            base_url=args.url, spring_data_mode=args.spring_data_mode
        )
        result = scraper.crawl_site(force_update=args.force_update)
        print("\n=== Scraping Summary ===")
        print(f"Pages processed: {result['valid_pages']}")
        print(f"Broken links found: {result['broken_links']}")
        print(f"Time taken: {result['duration_seconds']:.2f} seconds")
        sys.exit(0 if result["valid_pages"] else 1)
    except Exception as e:
        print(f"Fatal error during scraping: {e}")
        sys.exit(1)
    finally:
        if "scraper" in locals():
            scraper.close()


if __name__ == "__main__":
    main()
