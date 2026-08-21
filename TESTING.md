# OCR Testing Results

| Product | Date Format | Extracted Correctly? | Notes |
|---|---|---|---|
| Mehta Cosmetics jar | KI025 / 09/23 / 08/26 | Partial | # symbol dropped, "08/26" read correctly after crop fix |
| Weikfield jar | DE 25/NOV/27 | Partial | Spelled-month format not tested by regex yet (parser's job) |
| Reliance bottle | 31/01/2026, 30/07/2027 | ✅ after crop fix | Original fixed-box crop cut off digits; adjustable crop solved it |
| [add more] | | | |

## Summary
- Tested on X products, Y correctly extracted after the adjustable crop fix
- Main failure modes: thin symbols (@, #, /) dropped on low-quality print,
  character confusion (8↔B) on certain fonts, tilted/rotated photos before
  EXIF fix was added