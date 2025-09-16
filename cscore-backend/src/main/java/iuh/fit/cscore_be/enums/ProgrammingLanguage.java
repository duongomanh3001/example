package iuh.fit.cscore_be.enums;

public enum ProgrammingLanguage {
    JAVA(62, "java", "// Java code here\npublic class Main {\n    public static void main(String[] args) {\n        \n    }\n}"),
    PYTHON(71, "python3", "# Python code here\n"),
    CPP(54, "cpp", "// C++ code here\n#include <iostream>\nusing namespace std;\n\nint main() {\n    \n    return 0;\n}"),
    C(50, "c", "// C code here\n#include <stdio.h>\n\nint main() {\n    \n    return 0;\n}"),
    JAVASCRIPT(63, "javascript", "// JavaScript code here\n");
    
    private final int judge0Id;
    private final String name;
    private final String template;
    
    ProgrammingLanguage(int judge0Id, String name, String template) {
        this.judge0Id = judge0Id;
        this.name = name;
        this.template = template;
    }
    
    public int getJudge0Id() {
        return judge0Id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getTemplate() {
        return template;
    }
}
