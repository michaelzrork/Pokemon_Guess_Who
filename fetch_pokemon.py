import urllib.request
import json
import time
import os

IMAGE_BASE = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork"

def fetch_pokemon(pid):
    url = f"https://pokeapi.co/api/v2/pokemon/{pid}"
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=30) as resp:
        data = json.loads(resp.read().decode())

    name = data["name"].capitalize()
    # Handle special names
    if name == "Nidoran-f":
        name = "Nidoran F"
    elif name == "Nidoran-m":
        name = "Nidoran M"
    elif name == "Mr-mime":
        name = "Mr. Mime"
    elif name == "Farfetchd":
        name = "Farfetch'd"

    types = [t["type"]["name"].capitalize() for t in data["types"]]
    stats = {s["stat"]["name"]: s["base_stat"] for s in data["stats"]}

    return {
        "pokemonId": pid,
        "name": name,
        "imageUrl": f"{IMAGE_BASE}/{pid}.png",
        "types": types,
        "hp": stats["hp"],
        "attack": stats["attack"],
        "defense": stats["defense"],
        "spAtk": stats["special-attack"],
        "spDef": stats["special-defense"],
        "speed": stats["speed"]
    }

all_pokemon = []
for i in range(1, 152):
    try:
        p = fetch_pokemon(i)
        all_pokemon.append(p)
        print(f"Fetched #{i}: {p['name']}")
    except Exception as e:
        print(f"ERROR on #{i}: {e}")
    if i % 10 == 0:
        time.sleep(0.5)

assets_dir = os.path.join(
    r"C:\Users\micha\OneDrive\Documents\Workspace\Pokemon_Guess_Who",
    "app", "src", "main", "assets"
)
os.makedirs(assets_dir, exist_ok=True)

output_path = os.path.join(assets_dir, "pokemon_gen1.json")
with open(output_path, "w", encoding="utf-8") as f:
    json.dump(all_pokemon, f, indent=2, ensure_ascii=False)

print(f"\nDone! Wrote {len(all_pokemon)} Pokemon to {output_path}")
