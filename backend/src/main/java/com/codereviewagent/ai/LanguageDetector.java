package com.codereviewagent.ai;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class LanguageDetector {

    public String detectLanguage(String filename, String codeContent) {
        if (filename != null && !filename.isBlank()) {
            String lowerFile = filename.toLowerCase(Locale.ROOT);
            if (lowerFile.endsWith(".java")) return "Java";
            if (lowerFile.endsWith(".py")) return "Python";
            if (lowerFile.endsWith(".js") || lowerFile.endsWith(".jsx") || lowerFile.endsWith(".ts") || lowerFile.endsWith(".tsx")) return "JavaScript";
            if (lowerFile.endsWith(".html") || lowerFile.endsWith(".htm")) return "HTML";
            if (lowerFile.endsWith(".css")) return "CSS";
            if (lowerFile.endsWith(".cpp") || lowerFile.endsWith(".cxx") || lowerFile.endsWith(".cc") || lowerFile.endsWith(".c") || lowerFile.endsWith(".h") || lowerFile.endsWith(".hpp")) return "C++";
            if (lowerFile.endsWith(".sql")) return "SQL";
        }

        if (codeContent == null || codeContent.isBlank()) {
            return "Python";
        }

        String code = codeContent.trim();
        String codeLower = code.toLowerCase(Locale.ROOT);

        // Python detection (def, input(), print(), elif, import, etc.)
        if (codeLower.contains("input()") || codeLower.contains("input(") || codeLower.contains("print(") ||
            codeLower.contains("def ") || codeLower.contains("elif ") || codeLower.contains("self.") ||
            codeLower.contains("import numpy") || codeLower.contains("import pandas") || codeLower.contains("import os") ||
            codeLower.contains("if __name__ ==") || codeLower.contains("__init__") ||
            (codeLower.contains("int(") && codeLower.contains("input")) ||
            (code.contains(":") && !code.contains("{") && !code.contains(";"))) {
            return "Python";
        }

        // HTML detection
        if (codeLower.contains("<!doctype html") || codeLower.contains("<html") || codeLower.contains("<div") ||
            codeLower.contains("<body") || codeLower.contains("<span") || codeLower.contains("</")) {
            return "HTML";
        }

        // Java detection
        if (code.contains("public class ") || code.contains("System.out.print") || code.contains("import java.") ||
            code.contains("public static void main") || code.contains("@Override") || code.contains("@Entity") ||
            (code.contains("package ") && code.contains(";"))) {
            return "Java";
        }

        // C++ detection
        if (code.contains("#include ") || code.contains("std::cout") || code.contains("std::cin") ||
            code.contains("int main(") || code.contains("using namespace std")) {
            return "C++";
        }

        // SQL detection
        if ((codeLower.contains("select ") || codeLower.contains("insert into ") || codeLower.contains("update ") || codeLower.contains("create table ")) &&
            (codeLower.contains("from ") || codeLower.contains("where ") || codeLower.contains("values") || codeLower.contains("join "))) {
            return "SQL";
        }

        // JavaScript detection
        if (code.contains("const ") || code.contains("let ") || code.contains("var ") ||
            code.contains("function ") || code.contains("console.log") || code.contains("=>") ||
            code.contains("document.get")) {
            return "JavaScript";
        }

        // CSS detection
        if (code.contains("{") && code.contains("}") && (code.contains("color:") || code.contains("margin:") || code.contains("padding:") || code.contains("font-family:"))) {
            return "CSS";
        }

        // Fallback for code without C-style braces/semicolons is Python
        if (!code.contains("{") && !code.contains("}") && !code.contains(";")) {
            return "Python";
        }

        return "Python";
    }
}
