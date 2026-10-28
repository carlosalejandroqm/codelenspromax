package com.codelens.report.service;

import com.codelens.report.model.Report;
import org.springframework.stereotype.Component;

/**
 * Convierte el Markdown del reporte en un HTML autocontenido, listo para
 * imprimir o guardar como PDF desde el navegador.
 *
 * Es un conversor deliberadamente pequeno que cubre solo lo que el renderizador
 * Markdown genera (encabezados, tablas, listas, negritas, codigo y citas);
 * asi el servicio no arrastra una libreria de Markdown completa.
 */
@Component
public class HtmlReportRenderer {

    public String render(Report report) {
        String body = toHtml(report.getMarkdown() == null ? "" : report.getMarkdown());
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Reporte CodeLens - %s</title>
                  <style>
                    :root { color-scheme: light dark; }
                    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                           max-width: 60rem; margin: 0 auto; padding: 2.5rem 1.5rem; line-height: 1.65;
                           color: #1c1f23; background: #ffffff; }
                    h1 { font-size: 1.9rem; border-bottom: 2px solid #2d6cdf; padding-bottom: .5rem; }
                    h2 { font-size: 1.35rem; margin-top: 2.5rem; border-bottom: 1px solid #e3e6ea;
                         padding-bottom: .35rem; }
                    h3 { font-size: 1.1rem; margin-top: 1.8rem; }
                    table { border-collapse: collapse; width: 100%%; margin: 1rem 0; font-size: .92rem;
                            display: block; overflow-x: auto; }
                    th, td { border: 1px solid #d8dce1; padding: .5rem .7rem; text-align: left;
                             vertical-align: top; }
                    th { background: #f4f6f9; font-weight: 600; }
                    code { background: #f1f3f6; padding: .12rem .35rem; border-radius: 4px;
                           font-family: "SF Mono", Menlo, Consolas, monospace; font-size: .88em; }
                    blockquote { border-left: 4px solid #f0a13c; margin: 1rem 0; padding: .6rem 1rem;
                                 background: #fdf7ef; color: #5b4a30; }
                    ul { padding-left: 1.4rem; }
                    @media (prefers-color-scheme: dark) {
                      body { background: #14171a; color: #e6e9ee; }
                      th { background: #1e232a; }
                      th, td { border-color: #2c3239; }
                      code { background: #1e232a; }
                      h2 { border-color: #2c3239; }
                      blockquote { background: #241f17; color: #e2d3bb; }
                    }
                    @media print { body { max-width: none; padding: 0; } }
                  </style>
                </head>
                <body>
                %s
                </body>
                </html>
                """.formatted(escape(report.getProjectName()), body);
    }

    private String toHtml(String markdown) {
        StringBuilder html = new StringBuilder();
        String[] lines = markdown.split("\n", -1);
        boolean inList = false;
        boolean inTable = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                inList = closeList(html, inList);
                inTable = closeTable(html, inTable);
                continue;
            }

            if (trimmed.startsWith("|")) {
                // Una fila de separacion (|---|---|) no se emite.
                if (trimmed.matches("\\|[\\s:|-]+\\|")) {
                    continue;
                }
                inList = closeList(html, inList);
                boolean isHeaderRow = !inTable;
                if (isHeaderRow) {
                    html.append("<table>\n");
                    inTable = true;
                }
                html.append(tableRow(trimmed, isHeaderRow));
                continue;
            }
            inTable = closeTable(html, inTable);

            if (trimmed.startsWith("### ")) {
                inList = closeList(html, inList);
                html.append("<h3>").append(inline(trimmed.substring(4))).append("</h3>\n");
            } else if (trimmed.startsWith("## ")) {
                inList = closeList(html, inList);
                html.append("<h2>").append(inline(trimmed.substring(3))).append("</h2>\n");
            } else if (trimmed.startsWith("# ")) {
                inList = closeList(html, inList);
                html.append("<h1>").append(inline(trimmed.substring(2))).append("</h1>\n");
            } else if (trimmed.startsWith("> ")) {
                inList = closeList(html, inList);
                html.append("<blockquote>").append(inline(trimmed.substring(2))).append("</blockquote>\n");
            } else if (trimmed.startsWith("- ")) {
                if (!inList) {
                    html.append("<ul>\n");
                    inList = true;
                }
                html.append("<li>").append(inline(trimmed.substring(2))).append("</li>\n");
            } else if (trimmed.matches("^\\d+\\.\\s.*")) {
                if (!inList) {
                    html.append("<ul>\n");
                    inList = true;
                }
                html.append("<li>").append(inline(trimmed.replaceFirst("^\\d+\\.\\s", ""))).append("</li>\n");
            } else {
                inList = closeList(html, inList);
                html.append("<p>").append(inline(trimmed)).append("</p>\n");
            }
        }

        closeList(html, inList);
        closeTable(html, inTable);
        return html.toString();
    }

    private String tableRow(String line, boolean header) {
        String content = line.substring(1, line.endsWith("|") ? line.length() - 1 : line.length());
        String tag = header ? "th" : "td";
        StringBuilder row = new StringBuilder("<tr>");
        for (String cell : content.split("(?<!\\\\)\\|", -1)) {
            row.append("<").append(tag).append(">")
                    .append(inline(cell.trim().replace("\\|", "|")))
                    .append("</").append(tag).append(">");
        }
        return row.append("</tr>\n").toString();
    }

    private boolean closeList(StringBuilder html, boolean inList) {
        if (inList) {
            html.append("</ul>\n");
        }
        return false;
    }

    private boolean closeTable(StringBuilder html, boolean inTable) {
        if (inTable) {
            html.append("</table>\n");
        }
        return false;
    }

    /** Negritas, codigo y enlaces; el resto se escapa. */
    private String inline(String text) {
        String escaped = escape(text);
        escaped = escaped.replaceAll("`([^`]+)`", "<code>$1</code>");
        escaped = escaped.replaceAll("\\*\\*([^*]+)\\*\\*", "<strong>$1</strong>");
        escaped = escaped.replaceAll("&lt;(https?://[^&\\s]+)&gt;", "<a href=\"$1\">$1</a>");
        return escaped;
    }

    private String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
