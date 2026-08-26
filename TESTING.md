# OCR Testing Results

| Product | Date/Text Format | Extracted Correctly? | Notes |
|---|---|---|---|
| Mehta Cosmetics jar | `Mfg: 08/26, Exp: 09/2028` | Partial | `#` symbol dropped, "08/26" read correctly after crop fix |
| Weikfield jar | `OCT 24, SEP26` | Yes | Multiple dates extracted; relevant text passed to parser |
| Vaseline jar | `Mfg: 07/25, @ 06/28` | Yes | Adjustable crop helped focus on the date region |

## Parsing Testing Results

| Product | Relevant OCR Text | Parser Output | Result | Notes |
|---|---|---|---|---|
| Mehta Cosmetics jar | `Mfg: 08/26, Exp: 09/2028` | `09/2028` | Correct | Expiry label used to identify the relevant date |
| Weikfield jar | `OCT 24, SEP26` | `SEP26` | Correct | Parser selected the relevant expiry date from multiple dates |
| Vaseline jar | `Mfg: 07/25, @ 06/28` | `06/28` | Correct | Handles `@` as an expiry-related indicator |
| Dry Fruit packet | `APR 2026` | `APR 2026` | Correct | Parser successfully handled the text-based month and year format |

## Summary

- Tested the complete pipeline on 4 different product labels with different date formats.
- Adjustable cropping improved OCR accuracy by allowing the user to focus on the relevant text region.
- The OCR output was successfully passed to the rule-based parser for expiry-date identification.
- Main OCR failure modes included thin symbols such as `@`, `#`, and `/` being dropped, character confusion on certain fonts, poor image quality, and tilted or rotated images.
- The current parser handles common numeric and text-based date formats and selected expiry-related patterns.
- Current limitations include uncommon manufacturer-specific formats, ambiguous labels containing multiple dates, and indirect expiry information such as `Use Before X Months From Manufacturing`, which requires additional rule support.

