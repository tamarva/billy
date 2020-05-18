package com.billy.billy.camera;

import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Size;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleOnSubscribe;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ImageLoader {
    private ImageLoader() {
    }

    public static Size getImageSize(ExifInterface exifInterface) {
        final int DEFAULT_VALUE = -1;
        final int width = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, DEFAULT_VALUE);
        if (width == DEFAULT_VALUE) {
            return null;
        }
        final int height = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, DEFAULT_VALUE);
        if (height == DEFAULT_VALUE) {
            return null;
        }
        return new Size(width, height);
    }

    private static int getImageOrientation(ExifInterface exifInterface) {
        return exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
    }

    public static Bitmap rotateBitmap(Bitmap src, int exifOrientation) {
        int rotationDegrees = 0;

        switch (exifOrientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotationDegrees = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotationDegrees = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotationDegrees = 270;
                break;
            default:
                break;
        }

        if (rotationDegrees == 0) {
            return src;
        }

        // rotate bitmap
        Matrix matrix = new Matrix();
        matrix.preRotate(rotationDegrees);

        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }

    /**
     * From https://developer.android.com/topic/performance/graphics/load-bitmap
     */
    public static int calculateInSampleSize(int imageWidth, int imageHeight, int reqWidth, int reqHeight) {
        int inSampleSize = 1;

        if (imageHeight > reqHeight || imageWidth > reqWidth) {

            final int halfHeight = imageHeight / 2;
            final int halfWidth = imageWidth / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * From https://developer.android.com/topic/performance/graphics/load-bitmap
     */
    public static Bitmap decodeSampledBitmapFromUri(String imageUri, int reqWidth, int reqHeight) {
        ExifInterface exifInterface;
        try {
            exifInterface = new ExifInterface(imageUri);
        } catch (IOException e) {
            return null;
        }

        Size imageSize = getImageSize(exifInterface);
        if (imageSize == null) {
            return null;
        }

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = calculateInSampleSize(imageSize.getWidth(), imageSize.getHeight(), reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(imageUri, options);
        final int orientation = getImageOrientation(exifInterface);
        return rotateBitmap(bitmap, orientation);
    }

    public static Single<Bitmap> createImageLoaderSingle(final String uri, final Size finalSize) {
        return Single.create((SingleOnSubscribe<Bitmap>) emitter -> {
            Bitmap bitmap = ImageLoader.decodeSampledBitmapFromUri(uri, finalSize.getWidth(), finalSize.getHeight());
            if (bitmap != null) {
                emitter.onSuccess(bitmap);
            }
        }).subscribeOn(Schedulers.io());
    }
}
