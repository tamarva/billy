package com.billy.billy.text_recognition;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.firebase.ml.vision.text.FirebaseVisionText;

import java.util.ArrayList;
import java.util.List;

public class BillParserImpl implements BillParser {
    private static final String TAG = BillParserImpl.class.getSimpleName();
    private static final double DEFAULT_PRICE = 1.00;
    private static final int DEFAULT_AMOUNT = 1;
    private static final double DEFAULT_TOTAL = 0.0;
    private static final double FAILURE = -1;

    public BillParserImpl() {

    }

    @Override
    public Bill parse(@NonNull Context context, @NonNull FirebaseVisionText firebaseVisionText) {
        Preconditions.checkNotNull(firebaseVisionText);
        List<FirebaseVisionText.TextBlock> blocks = firebaseVisionText.getTextBlocks();

        if (blocks.isEmpty()) {
            return Bill.createEmpty();
        }

        List<FirebaseVisionText.Line> myList = new ArrayList<>();
        List<BillLine> billLines = new ArrayList<>();

        for (FirebaseVisionText.TextBlock block : blocks) {
            for (FirebaseVisionText.Line line : block.getLines()) {
                Log.d(TAG, "parse: line" + line.getText());
                myList.add(line);
            }
        }
        for (FirebaseVisionText.Line line : myList) {
            billLines.add(new BillLine(line));
        }

        List<BillLine> updatedBillLines = parseBillLine(myList, billLines);
        List<BillItem> billItems = parseByType(updatedBillLines);
        return Bill.create(billItems);
    }

    private List<BillLine> parseBillLine(@NonNull List<FirebaseVisionText.Line> myList, @NonNull List<BillLine> billLines) {
        Preconditions.checkNotNull(myList);
        Preconditions.checkNotNull(billLines);

        List<BillLine> updatedBillLines = new ArrayList<>();
        for (int i = 0; i < myList.size(); i++) {
            BillLine currBillLine = billLines.get(i);

            if (currBillLine.isAdded()) {
                continue;
            }

            for (int j = i; j < billLines.size(); j++) {
                BillLine otherBillLine = billLines.get(j);
                if (currBillLine == otherBillLine || otherBillLine.isAdded()) {
                    continue;
                }

                if (currBillLine.checkYCenterRange(otherBillLine)) {
                    otherBillLine.toggleAdded();
                    currBillLine.addOther(otherBillLine);
                }
            }
            updatedBillLines.add(currBillLine);
        }
        return updatedBillLines;
    }

    private List<BillItem> parseByType(@NonNull List<BillLine> updatedBillLines) {
        Preconditions.checkNotNull(updatedBillLines);
        List<BillItem> billItems = new ArrayList<>();
        double sumProduct = 0, totalOrder = 0;

        for (BillLine billLine : updatedBillLines) {
            int amount = DEFAULT_AMOUNT;
            String product = "";
            double price = DEFAULT_PRICE, total = DEFAULT_TOTAL;
            boolean totalLine = false, isNextTotal = false;

            for (FirebaseVisionText.Line line : billLine.getLines()) {
                for (FirebaseVisionText.Element element : line.getElements()) {
                    String elem = element.getText();
                    Log.d(TAG, "parseByType: element " + elem);

                    if (isNumeric(elem)) {
                        if (elem.contains("x")) {
                            amount = Integer.parseInt(elem.substring(0, elem.length() - 1));
                            continue;
                        }
                        amount = Integer.parseInt(elem);
                    } else if (isFood(elem)) {
                        if (!checkValidity(elem)) {
                            if (checkIfTotal(elem)) {
                                totalLine = true;
                                continue;
                            }
                            product += elem + " ";
                        }
                    } else {
                        if (!isCurrentlySign(elem)) {
                            if (totalLine) {
                                totalOrder = parsePriceToDouble(elem);
                                continue;
                            }
                            double tempTotal = parsePriceToDouble(elem);
                            if (isNextTotal) {
                                if (tempTotal != FAILURE) {
                                    total = tempTotal;
                                }
                            } else {
                                if (tempTotal != FAILURE) {
                                    price = tempTotal;
                                    isNextTotal = true;
                                }
                            }
                        }
                    }
                }

            }
            checkBillType(product, price, amount, total, billItems);
            if (total == DEFAULT_TOTAL && price != DEFAULT_PRICE) {
                sumProduct += price;
            } else {
                sumProduct += total;
            }
        }
        if (totalOrder != sumProduct) {
            return new ArrayList<>();
        }
        return billItems;
    }

    private boolean checkIfTotal(@NonNull String elem) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(elem));
        return elem.equals("TOTAL") || elem.equals("SUBTOTAL") || elem.equals("SUB-TOTAL") || elem.equals("SUB")
                || elem.equals("Total") || elem.equals("Subtotal");
    }

    private void checkBillType(@NonNull String product, double price, int amount, double total, @NonNull List<BillItem> billItems) {
        Preconditions.checkNotNull(billItems);
        if (product.equals("")) {
            return;
        }
        if (amount == DEFAULT_AMOUNT) {
            total = price;
            billItems.add(BillItem.create(amount, product, price, total));
            return;
        }
        if (total == DEFAULT_TOTAL) {
            billItems.add(BillItem.create(amount, product, getPricePerProduct(price, amount), price));
        } else {
            billItems.add(BillItem.create(amount, product, price, total));
        }
    }

    public boolean checkValidity(@NonNull String elem) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(elem));
        return (elem.equals("s") || elem.equals("S") || elem.equals("e") || elem.equals("E") || elem.equals("x") || elem.equals("X"));
    }

    private boolean isNumeric(@NonNull String str) {
        Log.d(TAG, "isNumeric: string " + str);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(str));
        return str.matches("[0-9]+x*");
    }

    public boolean isFood(@NonNull String str) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(str));
        return str.matches("^[a-zA-Z]+[a-zA-Z &\\-]*$");
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
                price = FAILURE;
            }
        } else {

            try {
                price = Double.parseDouble(tempPrice);
            } catch (NumberFormatException e) {
                price = FAILURE;
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

    private boolean isCurrentlySign(@NonNull String element) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(element));
        return element.length() <= 1 && Character.getType(element.charAt(0)) == Character.CURRENCY_SYMBOL;
    }
}
