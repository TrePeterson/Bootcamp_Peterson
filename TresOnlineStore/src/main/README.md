# Tre's Critical Hit Shop

A Java 17 command-line store for Baldur's Gate-inspired merch, Dragon Ball Z, One-Punch Man, and Death Note items. The 24-product catalog uses fictional demonstration prices for this classroom project.

## Open in IntelliJ

1. Extract the online-store folder from the ZIP.
2. Open its pom.xml in IntelliJ as a project.
3. Select Amazon Corretto 17 as the project SDK.
4. Run org.example.Main.

To copy the code into a fresh project instead, create an online-store Maven project and put all five Java files in src/main/java/org/example. Put products.csv in src/main/resources. Every Java file uses package org.example.

## Files

- Product.java: one item's SKU, name, price, and department.
- CartItem.java: one product, its quantity, and its line total.
- ShoppingCart.java: quantity merging, removal, item count, and total.
- FileManager.java: catalog loading and receipt saving.
- Main.java: screens, searches, input validation, checkout, and receipt formatting.
- src/main/resources/products.csv: editable themed catalog.

The classpath resource makes the catalog work on teammates' computers without a personal Downloads path. Keep the header SKU|Product Name|Price|Department and use one product per row.

## Features

The home screen opens products, the cart, or exits. Products can be searched by partial name, inclusive price range, or department. Names and SKUs ignore capitalization. Each search starts from the whole catalog; Show all products resets the displayed list.

Add-to-cart accepts a partial product name or an exact SKU. Common aliases DBZ and OPM also work. When several products match, the app shows numbered choices. Enter a manga volume/chapter (2, vol 2, or chapter 2), # followed by a displayed result number, a more specific keyword, or a listed SKU. B starts a new search and X cancels. A plain 2 means volume/chapter 2; #2 selects the second result. Only entries actually present in the catalog can be selected; the demo manga currently has Volumes 1 and 2.

Adding the same SKU combines quantities. Customers can remove part or all of a cart line. Prices and totals use BigDecimal so decimal money calculations remain exact.

Checkout accepts a simulated cash amount, checks payment sufficiency, shows the change, and asks for confirmation. A completed sale displays and saves the same receipt text, clears the cart, and returns home.

Receipts are saved under Receipts in the project's working directory, with filenames like yyyyMMdd-HHmmss.txt. Existing receipts are never overwritten. If saving fails, checkout does not complete and the cart remains available to retry.

The cart lasts for the current run. Completed receipts remain on disk. X cancels data entry; Press Enter pauses on confirmations and results.

## Presentation demo

1. Browse the catalog and search for Karlach.
2. Show the Manga department or search the exact price range 11.99 to 11.99.
3. Add BG1001 once, then add it again, and add DB1001 once.
4. Show the cart: one dice-set line with quantity 2, one manga line with quantity 1; total $61.97.
5. Try a $60 payment, then $100. The change is $38.03.
6. Confirm checkout, show the receipt file, and show the empty cart.

For branding, change STORE_NAME near the top of Main.java.

## Validation

Compiled and run with Amazon Corretto 17. Console checks covered searches, navigation, add/merge/remove, invalid and canceled input, insufficient payment, receipt saving, exact change, cart clearing, and a forced receipt-save failure. A separate 60-assertion harness checked model calculations, overflow, file validation, and receipt collision handling.
(Ai was used to create and develop this read me)
