package com.zyron.typewriter.view;

import android.view.View;
import android.inputmethodservice.InputMethodService;
import com.zyron.typewriter.widget.CodeEditor;

public class TextInputMethodService extends InputMethodService {
    private CodeEditor codeEditor;
    
    @Override
    public View onCreateInputView() {
        // TODO: Implement this method
        codeEditor = new CodeEditor(this);
        return codeEditor;
    }
    
}
