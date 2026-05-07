package com.evo.evo_backend.service;

import com.evo.evo_backend.model.AuditResponse;
import com.evo.evo_backend.model.CompetitorResult;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates PDF audit reports using pure HTML→PDF via OpenPDF (iText fork).
 * Add to pom.xml:
 *   <dependency>
 *     <groupId>com.github.librepdf</groupId>
 *     <artifactId>openpdf</artifactId>
 *     <version>1.3.30</version>
 *   </dependency>
 */
@Service
public class ReportService {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    // =========================
    // SINGLE AUDIT PDF
    // =========================
    public byte[] generateAuditPdf(AuditResponse audit, String url) {
        String html = buildAuditHtml(audit, url, null, false);
        return renderHtmlToPdf(html);
    }

    // =========================
    // WHITE-LABEL AUDIT PDF
    // =========================
    public byte[] generateAuditPdf(AuditResponse audit, String url,
                                    String agencyName) {
        String html = buildAuditHtml(audit, url, agencyName, true);
        return renderHtmlToPdf(html);
    }

    // =========================
    // COMPETITOR COMPARISON PDF
    // =========================
    public byte[] generateCompetitorPdf(CompetitorResult result,
                                         String primaryUrl,
                                         String competitorUrl) {
        String html = buildCompetitorHtml(result, primaryUrl, competitorUrl);
        return renderHtmlToPdf(html);
    }

    // =========================
    // BUILD AUDIT HTML
    // =========================
    private String buildAuditHtml(AuditResponse audit, String url,
                                   String agencyName, boolean whiteLabel) {

        String generatedAt = LocalDateTime.now().format(FMT);
        String brand       = whiteLabel && agencyName != null ? agencyName : "EVO AI Audit";
        String scoreColor  = scoreColor(audit.getScore());
        String visibility  = audit.getVisibility();

        StringBuilder sb = new StringBuilder();
        sb.append(htmlHead(brand));
        sb.append("<body>");

        // ── Header ──────────────────────────────────────────
        sb.append("<div class='header'>")
          .append("<div class='brand'>").append(escape(brand)).append("</div>")
          .append("<div class='meta'>Generated: ").append(generatedAt).append("</div>")
          .append("</div>");

        // ── Hero ─────────────────────────────────────────────
        sb.append("<div class='hero'>")
          .append("<h1>AI Visibility Audit</h1>")
          .append("<div class='url'>").append(escape(url)).append("</div>")
          .append("<div class='company'>").append(escape(audit.getCompanyName())).append("</div>")
          .append("</div>");

        // ── Score Cards ──────────────────────────────────────
        sb.append("<div class='cards'>")
          .append(card("Overall Score",    audit.getScore()    + "/100", scoreColor))
          .append(card("Potential Score",  audit.getPotentialScore() + "/100", "#2563eb"))
          .append(card("Improvement",      "+" + audit.getImprovement() + " pts", "#16a34a"))
          .append(card("AI Visibility",    visibility, visibilityColor(visibility)))
          .append("</div>");

        // ── Dimension Scores ─────────────────────────────────
        sb.append("<h2>Score Breakdown</h2>");
        sb.append("<table class='breakdown'>")
          .append("<tr><th>Dimension</th><th>Score</th><th>Rating</th></tr>")
          .append(dimensionRow("Content",        audit.getContentScore()))
          .append(dimensionRow("Performance",    audit.getPerformanceScore()))
          .append(dimensionRow("Accessibility",  audit.getAccessibilityScore()))
          .append(dimensionRow("Sitemap",        audit.getSitemapScore()))
          .append(dimensionRow("Social Meta",    audit.getSocialMetaScore()))
          .append(dimensionRow("Link Health",    audit.getLinkHealthScore()))
          .append("</table>");

        // ── Issues ───────────────────────────────────────────
        if (audit.getIssues() != null && !audit.getIssues().isEmpty()) {
            sb.append("<h2>Issues Found</h2><ul class='issues'>");
            for (String issue : audit.getIssues()) {
                sb.append("<li>").append(escape(issue)).append("</li>");
            }
            sb.append("</ul>");
        }

        // ── Suggestions ──────────────────────────────────────
        if (audit.getSuggestions() != null && !audit.getSuggestions().isEmpty()) {
            sb.append("<h2>Recommendations</h2><ol class='suggestions'>");
            for (String suggestion : audit.getSuggestions()) {
                sb.append("<li>").append(escape(suggestion)).append("</li>");
            }
            sb.append("</ol>");
        }

        // ── AI Analysis ──────────────────────────────────────
        if (audit.getAiAnalysis() != null) {
            sb.append("<h2>AI Analysis</h2>");
            sb.append("<div class='ai-summary'>")
              .append(escape(audit.getAiAnalysis().getSummary()))
              .append("</div>");

            List<String> improvements = audit.getAiAnalysis().getImprovements();
            if (improvements != null && !improvements.isEmpty()) {
                sb.append("<h3>AI Improvement Suggestions</h3><ol class='suggestions'>");
                for (String imp : improvements) {
                    sb.append("<li>").append(escape(imp)).append("</li>");
                }
                sb.append("</ol>");
            }
        }

        // ── Footer ───────────────────────────────────────────
        sb.append("<div class='footer'>")
          .append("Report generated by ").append(escape(brand))
          .append(" &bull; ").append(generatedAt)
          .append("</div>");

        sb.append("</body></html>");
        return sb.toString();
    }

    // =========================
    // BUILD COMPETITOR HTML
    // =========================
    private String buildCompetitorHtml(CompetitorResult result,
                                        String primaryUrl,
                                        String competitorUrl) {

        AuditResponse p = result.getPrimary();
        AuditResponse c = result.getCompetitor();
        String generatedAt = LocalDateTime.now().format(FMT);

        StringBuilder sb = new StringBuilder();
        sb.append(htmlHead("EVO AI Audit — Competitor Comparison"));
        sb.append("<body>");

        sb.append("<div class='header'>")
          .append("<div class='brand'>EVO AI Audit</div>")
          .append("<div class='meta'>Generated: ").append(generatedAt).append("</div>")
          .append("</div>");

        sb.append("<div class='hero'>")
          .append("<h1>Competitor Comparison</h1>")
          .append("</div>");

        // ── Side-by-side scores ──────────────────────────────
        sb.append("<table class='compare'>")
          .append("<tr><th>Metric</th>")
          .append("<th>").append(escape(p.getCompanyName())).append("</th>")
          .append("<th>").append(escape(c.getCompanyName())).append("</th>")
          .append("<th>Winner</th></tr>")
          .append(compareRow("Overall Score",   p.getScore(),            c.getScore()))
          .append(compareRow("Content",         p.getContentScore(),     c.getContentScore()))
          .append(compareRow("Performance",     p.getPerformanceScore(), c.getPerformanceScore()))
          .append(compareRow("Accessibility",   p.getAccessibilityScore(), c.getAccessibilityScore()))
          .append(compareRow("Sitemap",         p.getSitemapScore(),     c.getSitemapScore()))
          .append(compareRow("Social Meta",     p.getSocialMetaScore(),  c.getSocialMetaScore()))
          .append(compareRow("Link Health",     p.getLinkHealthScore(),  c.getLinkHealthScore()))
          .append("</table>");

        // ── Primary Wins ─────────────────────────────────────
        if (!result.getPrimaryWins().isEmpty()) {
            sb.append("<h2>&#127942; ").append(escape(p.getCompanyName())).append(" Advantages</h2><ul class='wins'>");
            for (String w : result.getPrimaryWins()) {
                sb.append("<li>").append(escape(w)).append("</li>");
            }
            sb.append("</ul>");
        }

        // ── Competitor Wins ──────────────────────────────────
        if (!result.getCompetitorWins().isEmpty()) {
            sb.append("<h2>&#9888; ").append(escape(c.getCompanyName())).append(" Advantages</h2><ul class='issues'>");
            for (String w : result.getCompetitorWins()) {
                sb.append("<li>").append(escape(w)).append("</li>");
            }
            sb.append("</ul>");
        }

        // ── Recommendations ──────────────────────────────────
        if (!result.getRecommendations().isEmpty()) {
            sb.append("<h2>Action Plan</h2><ol class='suggestions'>");
            for (String r : result.getRecommendations()) {
                sb.append("<li>").append(escape(r)).append("</li>");
            }
            sb.append("</ol>");
        }

        sb.append("<div class='footer'>Report generated by EVO AI Audit &bull; ")
          .append(generatedAt).append("</div>");

        sb.append("</body></html>");
        return sb.toString();
    }

    // =========================
    // HTML HEAD / STYLES
    // =========================
    private String htmlHead(String title) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
               "<title>" + escape(title) + "</title>" +
               "<style>" +
               "body{font-family:Arial,sans-serif;margin:0;padding:0;color:#1e293b;font-size:13px}" +
               ".header{background:#0f172a;color:#fff;padding:16px 32px;display:flex;justify-content:space-between;align-items:center}" +
               ".brand{font-size:18px;font-weight:700;letter-spacing:1px}" +
               ".meta{font-size:11px;color:#94a3b8}" +
               ".hero{background:#1e40af;color:#fff;padding:32px;text-align:center}" +
               ".hero h1{margin:0 0 8px;font-size:26px}" +
               ".url{font-size:12px;color:#93c5fd;margin-bottom:4px}" +
               ".company{font-size:16px;font-weight:600}" +
               ".cards{display:flex;gap:16px;padding:24px 32px;background:#f8fafc}" +
               ".card{flex:1;background:#fff;border:1px solid #e2e8f0;border-radius:8px;padding:16px;text-align:center}" +
               ".card-label{font-size:11px;color:#64748b;text-transform:uppercase;letter-spacing:0.5px;margin-bottom:6px}" +
               ".card-value{font-size:22px;font-weight:700}" +
               "h2{margin:24px 32px 8px;font-size:15px;color:#0f172a;border-bottom:2px solid #e2e8f0;padding-bottom:6px}" +
               "h3{margin:16px 32px 6px;font-size:13px;color:#334155}" +
               "table{width:calc(100% - 64px);margin:0 32px;border-collapse:collapse}" +
               "th{background:#0f172a;color:#fff;padding:8px 12px;text-align:left;font-size:12px}" +
               "td{padding:7px 12px;border-bottom:1px solid #f1f5f9;font-size:12px}" +
               "tr:nth-child(even) td{background:#f8fafc}" +
               ".badge{display:inline-block;padding:2px 10px;border-radius:12px;font-size:11px;font-weight:600}" +
               ".good{background:#dcfce7;color:#16a34a}" +
               ".medium{background:#fef9c3;color:#ca8a04}" +
               ".poor{background:#fee2e2;color:#dc2626}" +
               "ul,ol{margin:8px 32px 16px;padding-left:20px}" +
               "li{margin-bottom:5px;font-size:12px;line-height:1.6}" +
               ".issues li{color:#b91c1c}" +
               ".wins li{color:#15803d}" +
               ".suggestions li{color:#1d4ed8}" +
               ".ai-summary{margin:0 32px 16px;background:#f0f9ff;border-left:4px solid #2563eb;" +
               "padding:12px 16px;font-size:12px;line-height:1.7;border-radius:0 6px 6px 0}" +
               ".compare td:nth-child(2),.compare td:nth-child(3){text-align:center;font-weight:600}" +
               ".compare td:nth-child(4){text-align:center}" +
               ".footer{background:#0f172a;color:#64748b;font-size:10px;text-align:center;" +
               "padding:12px;margin-top:32px}" +
               "@media print{body{-webkit-print-color-adjust:exact}}" +
               "</style></head>";
    }

    // =========================
    // HTML HELPERS
    // =========================
    private String card(String label, String value, String color) {
        return "<div class='card'>" +
               "<div class='card-label'>" + escape(label) + "</div>" +
               "<div class='card-value' style='color:" + color + "'>" + escape(value) + "</div>" +
               "</div>";
    }

    private String dimensionRow(String name, int score) {
        String badge  = scoreBadge(score);
        String bar    = "<div style='background:#e2e8f0;border-radius:4px;height:6px;width:100%'>" +
                        "<div style='background:" + scoreColor(score) + ";width:" + score + "%;height:6px;border-radius:4px'></div></div>";
        return "<tr><td>" + name + "</td><td>" + score + "/100</td>" +
               "<td>" + bar + "</td><td>" + badge + "</td></tr>";
    }

    private String compareRow(String metric, int pScore, int cScore) {
        String winner;
        String wColor;
        if (pScore > cScore) {
            winner = "&#9650; Primary";
            wColor = "#16a34a";
        } else if (cScore > pScore) {
            winner = "&#9650; Competitor";
            wColor = "#dc2626";
        } else {
            winner = "&#8212; Tie";
            wColor = "#64748b";
        }
        return "<tr><td>" + metric + "</td>" +
               "<td style='color:" + scoreColor(pScore) + "'>" + pScore + "</td>" +
               "<td style='color:" + scoreColor(cScore) + "'>" + cScore + "</td>" +
               "<td style='color:" + wColor + ";font-size:11px;font-weight:600'>" + winner + "</td></tr>";
    }

    private String scoreBadge(int score) {
        if (score >= 75) return "<span class='badge good'>Good</span>";
        if (score >= 50) return "<span class='badge medium'>Moderate</span>";
        return "<span class='badge poor'>Needs Work</span>";
    }

    private String scoreColor(int score) {
        if (score >= 75) return "#16a34a";
        if (score >= 50) return "#ca8a04";
        return "#dc2626";
    }

    private String visibilityColor(String v) {
        if ("HIGH".equals(v))   return "#16a34a";
        if ("MEDIUM".equals(v)) return "#ca8a04";
        return "#dc2626";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    // =========================
    // HTML → PDF RENDERER
    // =========================
    private byte[] renderHtmlToPdf(String html) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            com.lowagie.text.Document pdfDoc =
                    new com.lowagie.text.Document(com.lowagie.text.PageSize.A4);

            com.lowagie.text.pdf.PdfWriter writer =
                    com.lowagie.text.pdf.PdfWriter.getInstance(pdfDoc, out);

            pdfDoc.open();

            // Parse HTML into PDF elements via XMLWorkerHelper
            com.lowagie.text.pdf.PdfContentByte cb = writer.getDirectContent();
            java.io.InputStream is = new java.io.ByteArrayInputStream(
                    html.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            com.lowagie.text.html.simpleparser.HTMLWorker worker =
                    new com.lowagie.text.html.simpleparser.HTMLWorker(pdfDoc);
            worker.parse(new java.io.InputStreamReader(is,
                    java.nio.charset.StandardCharsets.UTF_8));

            pdfDoc.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }
}