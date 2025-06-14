/*
 * YUI Compressor
 * http://developer.yahoo.com/yui/compressor/
 * Author: Julien Lecomte -  http://www.julienlecomte.net/
 * Copyright (c) 2011 Yahoo! Inc.  All rights reserved.
 * The copyrights embodied in the content of this file are licensed
 * by Yahoo! Inc. under the BSD (revised) open source license.
 */
package com.yahoo.platform.yui.compressor;

import java.util.*;

class ScriptOrFnScope {

    private int braceNesting;
    private ScriptOrFnScope parentScope;
    private ArrayList<ScriptOrFnScope> subScopes;
    private Hashtable<String,JavaScriptIdentifier> identifiers = new Hashtable<>();
    private Hashtable<String, String> hints = new Hashtable<>();
    private boolean markedForMunging = true;
    private int varcount = 0;

    ScriptOrFnScope(int braceNesting, ScriptOrFnScope parentScope) {
        this.braceNesting = braceNesting;
        this.parentScope = parentScope;
        this.subScopes = new ArrayList<>();
        if (parentScope != null) {
            parentScope.subScopes.add(this);
        }
    }

    int getBraceNesting() {
        return braceNesting;
    }

    ScriptOrFnScope getParentScope() {
        return parentScope;
    }

    JavaScriptIdentifier declareIdentifier(String symbol) {
        JavaScriptIdentifier identifier = (JavaScriptIdentifier) identifiers.get(symbol);
        if (identifier == null) {
            identifier = new JavaScriptIdentifier(symbol, this);
            identifiers.put(symbol, identifier);
        }
        return identifier;
    }

    JavaScriptIdentifier getIdentifier(String symbol) {
        return (JavaScriptIdentifier) identifiers.get(symbol);
    }

    void addHint(String variableName, String variableType) {
        hints.put(variableName, variableType);
    }

    void preventMunging() {
        if (parentScope != null) {
            // The symbols in the global scope don't get munged,
            // but the sub-scopes it contains do get munged.
            markedForMunging = false;
        }
    }

    private ArrayList<String> getUsedSymbols() {
        ArrayList<String> result = new ArrayList<>();
        Enumeration<JavaScriptIdentifier> elements = identifiers.elements();
        while (elements.hasMoreElements()) {
            JavaScriptIdentifier identifier = (JavaScriptIdentifier) elements.nextElement();
            String mungedValue = identifier.getMungedValue();
            if (mungedValue == null) {
                mungedValue = identifier.getValue();
            }
            result.add(mungedValue);
        }
        return result;
    }

    private ArrayList getAllUsedSymbols() {
        ArrayList<String> result = new ArrayList<>();
        ScriptOrFnScope scope = this;
        while (scope != null) {
            result.addAll(scope.getUsedSymbols());
            scope = scope.parentScope;
        }
        return result;
    }

    int incrementVarCount() {
        varcount++;
        return varcount;
    }

    public void getFullMapping(StringBuffer outBuffer, String mungedPrefix) {
        Enumeration elements = identifiers.elements();
        while (elements.hasMoreElements()) {
            JavaScriptIdentifier identifier = (JavaScriptIdentifier) elements.nextElement();
            String mungedValue = identifier.getMungedValue();
            if (mungedValue == null) {
                mungedValue = identifier.getValue();
            }
            outBuffer.append(mungedPrefix + mungedValue);
            outBuffer.append(": ");
            outBuffer.append(identifier.getValue() + "\n");
        }

        for (int i = 0; i < subScopes.size(); i++) {
            ScriptOrFnScope scope = (ScriptOrFnScope) subScopes.get(i);
            scope.getFullMapping(outBuffer, "\t"+mungedPrefix);
        }
    }

    void munge() {

        if (!markedForMunging) {
            // Stop right here if this scope was flagged as unsafe for munging.
            return;
        }

        int pickFromSet = 1;

        // Do not munge symbols in the global scope!
        if (parentScope != null) {

            LinkedHashSet<String> freeSymbols = new LinkedHashSet<>();

            freeSymbols.addAll(JavaScriptCompressor.ones);
            freeSymbols.removeAll(getAllUsedSymbols());
            if (freeSymbols.size() == 0) {
                pickFromSet = 2;
                freeSymbols.addAll(JavaScriptCompressor.twos);
                freeSymbols.removeAll(getAllUsedSymbols());
            }
            if (freeSymbols.size() == 0) {
                pickFromSet = 3;
                freeSymbols.addAll(JavaScriptCompressor.threes);
                freeSymbols.removeAll(getAllUsedSymbols());
            }
            if (freeSymbols.size() == 0) {
                throw new IllegalStateException("The YUI Compressor ran out of symbols. Aborting...");
            }

            Enumeration elements = identifiers.elements();
            while (elements.hasMoreElements()) {
                if (freeSymbols.size() == 0) {
                    pickFromSet++;
                    if (pickFromSet == 2) {
                        freeSymbols.addAll(JavaScriptCompressor.twos);
                    } else if (pickFromSet == 3) {
                        freeSymbols.addAll(JavaScriptCompressor.threes);
                    } else {
                        throw new IllegalStateException("The YUI Compressor ran out of symbols. Aborting...");
                    }
                    // It is essential to remove the symbols already used in
                    // the containing scopes, or some of the variables declared
                    // in the containing scopes will be redeclared, which can
                    // lead to errors.
                    freeSymbols.removeAll(getAllUsedSymbols());
                }

                String mungedValue;
                JavaScriptIdentifier identifier = (JavaScriptIdentifier) elements.nextElement();
                if (identifier.isMarkedForMunging()) {
                    Iterator freeSymIt = freeSymbols.iterator();
                    mungedValue = (String) freeSymIt.next();
                    freeSymIt.remove();
                } else {
                    mungedValue = identifier.getValue();
                }
                identifier.setMungedValue(mungedValue);
            }
        }

        for (int i = 0; i < subScopes.size(); i++) {
            ScriptOrFnScope scope = (ScriptOrFnScope) subScopes.get(i);
            scope.munge();
        }
    }
}
