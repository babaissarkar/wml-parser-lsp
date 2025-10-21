import requests
from bs4 import BeautifulSoup

# Define the base URL
base_url = "https://wiki.wesnoth.org"

# URL of the webpage to scrape
url = f"{base_url}/ReferenceWML"

# Fetch the web page
response = requests.get(url)
soup = BeautifulSoup(response.content, 'html.parser')

# Select all <tr> elements except the first one
rows = soup.find_all('tr')[1:]

# Prepare data for properties file
properties_data = []
for row in rows:
    links = row.find_all('a')
    for link in links:
        full_link = base_url + link['href']  # Create full link
        properties_data.append(f"{link.get_text()}={full_link}")  # Prepare properties format

# Save to properties file
with open('src/main/resources/taglinks.properties', 'w', encoding='utf-8') as properties_file:
    for entry in properties_data:
        properties_file.write(entry + '\n')  # Write each line

print(f"Saved {len(properties_data)} links to 'src/main/resources/taglinks.properties'.")
