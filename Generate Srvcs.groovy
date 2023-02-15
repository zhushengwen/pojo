import com.intellij.database.model.DasTable
import com.intellij.database.model.ObjectKind
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil
import java.io.*
import java.text.SimpleDateFormat

/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */
packageName = ""
modelClassName = ""
repoClassName = ""
serviceClassName = ""


//FILES.chooseDirectoryAndSave("Choose service directory", "Choose where to store generated files") { dir ->
//    SELECTION.filter { it instanceof DasTable && it.getKind() == ObjectKind.TABLE }.each { generate(it, dir) }
//}
dir = "C:\\soft\\java\\code\\src\\main\\java\\com\\jeiat\\itapi\\modules\\pom\\service"
SELECTION.filter { it instanceof DasTable && it.getKind() == ObjectKind.TABLE }.each { generate(it, dir) }

def generate(table, dir) {
    def className = javaClassName(table.getName(), true)
    packageName = getPackageName(dir)
    def s = packageName.split(/\./)
    modelClassName = s[0..-2].join(".") + ".model." + className
    repoClassName = s[0..-2].join(".") + ".repo." + className + "Repository"
    serviceClassName = s[0..-2].join(".") + ".service." + className + "Service"

    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(dir, className + "Service.java")), "UTF-8"))
    printWriter.withPrintWriter { out -> generate(out, className, table) }

    //s.each { ele -> out.println ele }

    String implDir = dir.toString() + "/impl"
    packageName = getPackageName(implDir)
    def file = new File(implDir)
    if(!file.exists()){
        file.mkdir()
    }
    printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(implDir, className + "ServiceImpl.java")), "UTF-8"))
    printWriter.withPrintWriter { out -> generateImpl(out, className, table) }
}

// 获取包所在文件夹路径
def getPackageName(dir) {
    return dir.toString().replaceAll("\\\\", ".").replaceAll("/", ".").replaceAll("^.*src(\\.main\\.java\\.)?", "") + ";"
}

def generate(out, className, table) {

    def javaName = javaName(className,false)

    out.println "package $packageName"
    out.println ""
    out.println "import $modelClassName;"
    out.println "import org.springframework.data.jpa.domain.Specification;"
    out.println "import java.util.List;"

    out.println ""
    out.println "/**\n" +
            " * Date " + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + " \n" +
            " */"
    out.println ""
    out.println "public interface ${className}Service {"
    out.println ""

    out.println "    List<$className> get${className}s(Specification<$className> spec);"
    out.println ""

    out.println "    ${className} save${className}(${className} $javaName);"
    out.println ""

    out.println "    ${className} update${className}(${className} $javaName);"
    out.println ""

    out.println "    void delete${className}(Long id);"
    out.println ""
    out.println "}"
}
def generateImpl(out, className, table) {

    def javaName = javaName(className,false)

    out.println "package $packageName"
    out.println ""
    out.println "import $modelClassName;"
    out.println "import $repoClassName;"
    out.println "import $serviceClassName;"
    out.println "import org.springframework.beans.factory.annotation.Autowired;"
    out.println "import org.springframework.data.jpa.domain.Specification;"
    out.println "import org.springframework.stereotype.Service;"

    out.println "import java.util.List;"

    out.println ""
    out.println "/**\n" +
            " * Date " + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + " \n" +
            " */"
    out.println ""
    out.println "@Service"
    out.println "public class ${className}ServiceImpl implements ${className}Service {"
    out.println ""
    out.println "    @Autowired\n" +
                "    ${className}Repository ${javaName}Repository;"
    out.println ""
    out.println "    @Override\n" +
                "    public List<${className}> get${className}s(Specification<${className}> spec) {\n" +
                "        return ${javaName}Repository.findAll(spec);\n" +
                "    }"
    out.println ""
    out.println "    @Override\n" +
                "    public ${className} save${className}(${className} ${javaName}) {\n" +
                "        return ${javaName}Repository.save(${javaName});\n" +
                "    }\n"
    out.println ""
    out.println "    @Override\n" +
                "    public ${className} update${className}(${className} ${javaName}) {\n" +
                "        return save${className}(${javaName});\n" +
                "    }\n"
    out.println ""
    out.println "    @Override\n" +
                "    public void delete${className}(Long id) {\n" +
                "        ${javaName}Repository.deleteById(id);\n" +
                "    }\n"
    out.println "}"
}

def javaClassName(str, capitalize) {
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
            .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}

def javaName(str, capitalize) {
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
            .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}

def isNotEmpty(content) {
    return content != null && content.toString().trim().length() > 0
}
