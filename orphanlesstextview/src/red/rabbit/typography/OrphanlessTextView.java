package red.rabbit.typography;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * This TextView extension will try its best to cure typographic orphans, i.e.
 * preventing a word from appearing by itself on the last line of the View.
 *
 * The result is undefined when {@link #setLines(int)} is called or
 * {@link #setHorizontallyScrolling(boolean)} set to True.
 *
 * @author  Nathanael Silverman
 *
 */
public class OrphanlessTextView extends TextView {

    private static CharSequence LINE_SEPARATOR = System.getProperty("line.separator");

    public OrphanlessTextView(Context context) {
        super(context);
        init();
    }

    public OrphanlessTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        addTextChangedListener(mTextWatcher);
    }

    private void cureOrphan() {
        cureOrphan(getText().toString());
    }

    private void cureOrphan(String text) {
        text = text.trim();

        final Drawable[] drawables = getCompoundDrawables();
        final int viewWidth = getWidth() - getPaddingLeft() - getPaddingRight()
                // Account for left drawable
                - (drawables[0] != null ? drawables[0].getIntrinsicWidth() + getCompoundPaddingLeft() : 0)
                // Account for right drawable
                - (drawables[2] != null ? drawables[2].getIntrinsicWidth() + getCompoundPaddingRight() : 0);

        if (!TextUtils.isEmpty(text) && viewWidth > 0) {
            final float[] widths = new float[text.length()];
            getPaint().getTextWidths(text.toString(), widths);

            float lineWidth = 0;
            int lastSpaceAt = 0;
            int secondToLastSpaceAt = 0;
            int lastWordWidth = 0;
            int secondToLastWordWidth = 0;
            int numWordsOnLastLine = 1;
            int lineCount = 1;

            for (int i = 0; i < widths.length; i++) {
                char c = text.charAt(i);
                final float charWidth = widths[i];
                lineWidth += charWidth;

                if (c == ' ') {
                    secondToLastSpaceAt = lastSpaceAt;
                    lastSpaceAt = i;
                    secondToLastWordWidth = lastWordWidth;
                    lastWordWidth = 0;
                    numWordsOnLastLine++;

                    // Skip spaces if more than one
                    while (text.charAt(i + 1) == ' ') {
                        i++;
                    }
                    c = text.charAt(i);

                } else {
                    lastWordWidth += charWidth;
                }

                // Moving on to the next line
                if (text.subSequence(i, i + LINE_SEPARATOR.length()).equals(LINE_SEPARATOR) || (lineWidth > viewWidth && c != ' ')) {
                    numWordsOnLastLine = 1;
                    lineCount++;
                    lineWidth = charWidth;
                }
            }

            if (lineCount > 1
                    && secondToLastSpaceAt != 0  // More than one word
                    && numWordsOnLastLine == 1
                    // Last two words will fit on a single line
                    && lastWordWidth + secondToLastWordWidth + widths[lastSpaceAt] < viewWidth
                    // Make sure we're not about to go over the line limit
                    && lineCount < getMaxLines()
                    ) {
                setText(text.substring(0, secondToLastSpaceAt) + LINE_SEPARATOR + text.substring(secondToLastSpaceAt));
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) cureOrphan();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        cureOrphan();
    }

    private final TextWatcher mTextWatcher = new TextWatcher() {

        @Override
        public void afterTextChanged(Editable s) {
            cureOrphan();
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }
    };

    @Override
    @SuppressLint("NewApi")
    public int getMaxLines() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN ?
                super.getMaxLines() : Integer.MAX_VALUE;
    }
}
