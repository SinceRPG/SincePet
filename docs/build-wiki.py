from __future__ import annotations

import html
import re
import shutil
import sys
from pathlib import Path


PAGES = [
    ("index.md", "Home"),
    ("installation.md", "Installation"),
    ("commands-permissions.md", "Commands"),
    ("configuration.md", "Configuration"),
    ("pets.md", "Pets"),
    ("upgrades.md", "Upgrades"),
    ("gui.md", "GUI"),
    ("display-conditions.md", "Conditions"),
    ("messages.md", "Messages"),
    ("database.md", "Database"),
    ("worldguard.md", "WorldGuard"),
    ("mythicmobs.md", "MythicMobs"),
    ("troubleshooting.md", "Troubleshooting"),
]


def inline(text: str) -> str:
    escaped = html.escape(text)
    escaped = re.sub(r"`([^`]+)`", r"<code>\1</code>", escaped)
    escaped = re.sub(r"\*\*([^*]+)\*\*", r"<strong>\1</strong>", escaped)
    escaped = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", lambda m: f'<a href="{link_target(m.group(2))}">{m.group(1)}</a>', escaped)
    return escaped


def link_target(target: str) -> str:
    if target.endswith(".md"):
        return target[:-3] + ".html"
    if ".md#" in target:
        return target.replace(".md#", ".html#")
    return target


def render_markdown(markdown: str) -> str:
    lines = markdown.splitlines()
    out: list[str] = []
    in_code = False
    in_ul = False
    in_ol = False
    i = 0

    def close_lists() -> None:
        nonlocal in_ul, in_ol
        if in_ul:
            out.append("</ul>")
            in_ul = False
        if in_ol:
            out.append("</ol>")
            in_ol = False

    while i < len(lines):
        line = lines[i]

        if line.startswith("```"):
            close_lists()
            if in_code:
                out.append("</code></pre>")
                in_code = False
            else:
                out.append("<pre><code>")
                in_code = True
            i += 1
            continue

        if in_code:
            out.append(html.escape(line))
            i += 1
            continue

        if not line.strip():
            close_lists()
            i += 1
            continue

        if line.lstrip().startswith("<") and line.rstrip().endswith(">"):
            close_lists()
            out.append(line)
            i += 1
            continue

        if is_table_start(lines, i):
            close_lists()
            headers = split_table_row(lines[i])
            aligns = split_table_row(lines[i + 1])
            out.append("<table><thead><tr>")
            for header in headers:
                out.append(f"<th>{inline(header)}</th>")
            out.append("</tr></thead><tbody>")
            i += 2
            while i < len(lines) and lines[i].strip().startswith("|"):
                cells = split_table_row(lines[i])
                out.append("<tr>")
                for cell in cells:
                    out.append(f"<td>{inline(cell)}</td>")
                out.append("</tr>")
                i += 1
            out.append("</tbody></table>")
            continue

        heading = re.match(r"^(#{1,6})\s+(.*)$", line)
        if heading:
            close_lists()
            level = len(heading.group(1))
            text = heading.group(2)
            anchor = slug(text)
            out.append(f'<h{level} id="{anchor}">{inline(text)}</h{level}>')
            i += 1
            continue

        bullet = re.match(r"^\s*-\s+(.*)$", line)
        if bullet:
            if in_ol:
                out.append("</ol>")
                in_ol = False
            if not in_ul:
                out.append("<ul>")
                in_ul = True
            out.append(f"<li>{inline(bullet.group(1))}</li>")
            i += 1
            continue

        numbered = re.match(r"^\s*\d+\.\s+(.*)$", line)
        if numbered:
            if in_ul:
                out.append("</ul>")
                in_ul = False
            if not in_ol:
                out.append("<ol>")
                in_ol = True
            out.append(f"<li>{inline(numbered.group(1))}</li>")
            i += 1
            continue

        close_lists()
        out.append(f"<p>{inline(line)}</p>")
        i += 1

    close_lists()
    if in_code:
        out.append("</code></pre>")
    return "\n".join(out)


def is_table_start(lines: list[str], index: int) -> bool:
    if index + 1 >= len(lines):
        return False
    return lines[index].strip().startswith("|") and re.match(r"^\s*\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)+\|?\s*$", lines[index + 1]) is not None


def split_table_row(line: str) -> list[str]:
    return [part.strip() for part in line.strip().strip("|").split("|")]


def slug(text: str) -> str:
    value = re.sub(r"[^a-zA-Z0-9\s-]", "", text).strip().lower()
    return re.sub(r"\s+", "-", value)


def page_name(source: str) -> str:
    return "index.html" if source == "index.md" else source[:-3] + ".html"


def nav_html(active: str) -> str:
    links = []
    for source, label in PAGES:
        href = page_name(source)
        cls = ' class="active"' if href == active else ""
        links.append(f'<a{cls} href="{href}">{html.escape(label)}</a>')
    return "\n".join(links)


def layout(title: str, body: str, active: str, is_home: bool) -> str:
    hero = ""
    if is_home:
        hero = """
        <section class="hero">
          <h1>SincePet Wiki</h1>
          <p>A complete configuration guide for pets, upgrades, GUI items, display conditions, riding, combat, data storage, and integrations.</p>
        </section>
        """
    return f"""<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>{html.escape(title)} - SincePet Wiki</title>
  <link rel="stylesheet" href="wiki.css">
</head>
<body>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">
        <a class="brand-title" href="index.html">SincePet</a>
        <div class="brand-subtitle">Configuration Wiki</div>
      </div>
      <nav class="nav">
        {nav_html(active)}
      </nav>
    </aside>
    <main class="content">
      {hero}
      <article class="article">
        {body}
      </article>
    </main>
  </div>
</body>
</html>
"""


def main() -> int:
    source_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("docs")
    output_dir = Path(sys.argv[2]) if len(sys.argv) > 2 else Path("wiki")
    if output_dir.exists() and source_dir.resolve() != output_dir.resolve():
        shutil.rmtree(output_dir)
        output_dir.mkdir(parents=True)
        shutil.copy2(source_dir / "wiki.css", output_dir / "wiki.css")
    elif not output_dir.exists():
        output_dir.mkdir(parents=True)
        shutil.copy2(source_dir / "wiki.css", output_dir / "wiki.css")

    for source, label in PAGES:
        text = (source_dir / source).read_text(encoding="utf-8")
        body = render_markdown(text)
        active = page_name(source)
        (output_dir / active).write_text(layout(label, body, active, source == "index.md"), encoding="utf-8")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
