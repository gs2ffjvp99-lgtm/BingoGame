package com.example.bingogame.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridLayout;

/**
 * A {@link GridLayout} that is always square (width == height).
 *
 * <p>The Bingo card uses it so all 25 cells stay square on phones, tablets
 * and in landscape. It takes the smaller of the available width and height
 * as the side length.</p>
 */
public class SquareGridLayout extends GridLayout {

    /** Constructor used when the view is created in Java code. */
    public SquareGridLayout(Context context) {
        super(context);
    }

    /** Constructor used when the view is inflated from an XML layout. */
    public SquareGridLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /** Constructor used when a style attribute is also given. */
    public SquareGridLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Called by Android to ask "how big do you want to be?".
     * We answer with a square that fits in the space we were offered.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        // UNSPECIFIED height (e.g. inside a ScrollView) means "any height":
        // then the width alone decides the size.
        boolean heightIsFree = MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED;
        int side = heightIsFree ? width : Math.min(width, height);

        // Measure the grid (and its cells) with exactly side x side pixels.
        int squareSpec = MeasureSpec.makeMeasureSpec(side, MeasureSpec.EXACTLY);
        super.onMeasure(squareSpec, squareSpec);
    }
}
