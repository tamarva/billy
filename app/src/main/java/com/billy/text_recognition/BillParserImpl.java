package com.billy.text_recognition;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.firebase.ml.vision.text.FirebaseVisionText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BillParserImpl implements BillParser {
    private static final String TAG = BillParserImpl.class.getSimpleName();
    private static final double DEFAULT_PRICE = 1.00;
    private List<BillItem> billItems = new ArrayList<>();

    public BillParserImpl() {

    }

    @Override
    @NonNull public Bill parse(@NonNull Context context, @NonNull FirebaseVisionText firebaseVisionText) {
        Preconditions.checkNotNull(firebaseVisionText);
        List<FirebaseVisionText.TextBlock> blocks = firebaseVisionText.getTextBlocks();

        if (blocks.isEmpty()) {
            return Bill.createEmpty();
        }

        for (int blockIndex = 0; blockIndex < blocks.size(); blockIndex++) {
            List<FirebaseVisionText.Line> lines = blocks.get(blockIndex).getLines();
            for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                List<FirebaseVisionText.Element> elements = lines.get(lineIndex).getElements();

                String element = elements.get(0).getText();
                if (isAlpha(element)) {
                    if (checkForPriceExistence(elements)) {
                        Log.d(TAG, "parse: full block no amount");
                        fullBlockProductPrice(elements);
                    } else {
                        Log.d(TAG, "parse: line consists two blocks no amount");
                        if (blockIndex + 1 < blocks.size()) {
                            numericCase(blockIndex, blocks);
                            if (blockIndex + 2 < blocks.size()) {
                                blockIndex++;
                            }
                        } else {
                            Toast.makeText(context, "please re-scan bill", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        }
        return Bill.create(billItems);
    }

    private void numericCase(int blockIndex, @NonNull List<FirebaseVisionText.TextBlock> blocks) {
        Preconditions.checkNotNull(blocks);
        String nameOfProduct = blocks.get(blockIndex).getText();
        String priceOfProduct = blocks.get(blockIndex + 1).getText();
        separateBlocksProductPrice(nameOfProduct, priceOfProduct);
    }

    private void fullBlockProductPrice(@NonNull List<FirebaseVisionText.Element> elements) {
        Preconditions.checkNotNull(elements);
        Map<Double, String> products = getProducts(elements);
        for (Map.Entry<Double, String> entry : products.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                BillItem billItem = BillItem.create(entry.getValue(), getPricePerProduct(entry.getKey(), 1), 1);
                billItems.add(billItem);
            }
        }
    }

    private void separateBlocksProductPrice(@NonNull String name, @NonNull String price) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(price));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));
        double pricePerProduct = getPricePerProduct(parsePriceToDouble(price), 1);
        billItems.add(BillItem.create(name, pricePerProduct, 1));
    }

    private boolean checkForPriceExistence(@NonNull List<FirebaseVisionText.Element> elements) {
        Preconditions.checkNotNull(elements);
        for (FirebaseVisionText.Element element : elements) {
            if (element.getText().contains(".")) {
                return true;
            }
        }
        return false;
    }

    private boolean isNumeric(@NonNull String str) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(str));
        return str.matches("[0-9]+");
    }

    public boolean isAlpha(@NonNull String str) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(str));
        return str.matches("^[a-zA-Z]+[a-zA-Z \\-]*$");
    }

    private double getPricePerProduct(double price, int amount) {
        return price / amount;
    }

    private double parsePriceToDouble(@NonNull String tempPrice) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(tempPrice));
        double price;
        if (tempPrice.contains("s") || tempPrice.contains("S") || isContainsCurrency(tempPrice.trim())) {
            try {
                price = Double.parseDouble(tempPrice.substring(1));
            } catch (NumberFormatException e) {
                price = DEFAULT_PRICE;
            }
        } else {

            try {
                price = Double.parseDouble(tempPrice);
            } catch (NumberFormatException e) {
                price = DEFAULT_PRICE;
            }
        }
        return price;
    }

    private boolean isContainsCurrency(@NonNull String value) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(value));
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.getType(c) == Character.CURRENCY_SYMBOL) {
                return true;
            }
        }
        return false;
    }

    private Map<Double, String> getProducts(@NonNull List<FirebaseVisionText.Element> elements) {
        Preconditions.checkNotNull(elements);
        Map<Double, String> map = new HashMap<>();
        double price = DEFAULT_PRICE;
        String product = "";
        for (int elementIndex = 0; elementIndex < elements.size(); elementIndex++) {
            String element = elements.get(elementIndex).getText().trim();
            Log.d(TAG, "getProducts: element" + element);
            if (isAlpha(element)) {
                product += element + " ";
            } else {
                if (!isCurrentlySign(element)) {
                    price = parsePriceToDouble(element);
                }
            }
        }
        map.put(price, product);
        return map;
    }

    private boolean isCurrentlySign(@NonNull String element) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(element));
        return element.length() <= 1 && Character.getType(element.charAt(0)) == Character.CURRENCY_SYMBOL;
    }
}
