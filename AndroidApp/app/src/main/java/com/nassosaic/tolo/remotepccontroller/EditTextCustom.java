package com.nassosaic.tolo.remotepccontroller;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;

public class EditTextCustom extends androidx.appcompat.widget.AppCompatEditText
{
    public EditTextCustom( Context context )
    {
        super( context );
    }

    public EditTextCustom(Context context, AttributeSet attribute_set )
    {
        super( context, attribute_set );
    }

    public EditTextCustom( Context context, AttributeSet attribute_set, int def_style_attribute )
    {
        super( context, attribute_set, def_style_attribute );
    }

    @Override
    public boolean onKeyPreIme( int key_code, KeyEvent event )
    {
        if ( key_code == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP ) {
            this.clearFocus();
        }

        return super.onKeyPreIme( key_code, event );
    }
}