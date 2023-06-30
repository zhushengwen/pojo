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
moduleName = ""
packageName = ""
modelClassName = ""
repoClassName = ""
serviceClassName = ""
typeMapping = [
        (~/(?i)tinyint|smallint|mediumint/)      : "Long",
        (~/(?i)int/)                             : "Long",
        (~/(?i)bool|bit/)                        : "Boolean",
        (~/(?i)float|double|decimal|real/)       : "Double",
        (~/(?i)datetime|timestamp|date|time/)    : "Date",
        (~/(?i)blob|binary|bfile|clob|raw|image/): "InputStream",
        (~/(?i)/)                                : "String"
]

//FILES.chooseDirectoryAndSave("Choose service directory", "Choose where to store generated files") { dir ->
//    SELECTION.filter { it instanceof DasTable && it.getKind() == ObjectKind.TABLE }.each { generate(it, dir) }
//}
dir = ""
SELECTION.filter { it instanceof DasTable && it.getKind() == ObjectKind.TABLE }.each { generate(it, dir) }

def generate(table, dir) {
    moduleName = table.getName().split(/_/)[0]
    dir = getProjectName(PROJECT.toString()) + "\\src\\main\\java\\com\\jeiat\\itapi\\modules\\"+moduleName+"\\service"
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
    out.println "import org.springframework.data.domain.Page;"
    out.println "import org.springframework.data.domain.Pageable;"
    out.println "import org.springframework.data.domain.Sort;"
    out.println "import org.springframework.data.jpa.domain.Specification;"

    out.println "import java.util.List;"

    out.println ""
    out.println "/**\n" +
            " * Date " + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + " \n" +
            " */"
    out.println ""
    out.println "public interface ${className}Service {"
    out.println ""

    out.println "    Page<$className> get${className}Page(Specification<$className> spec, Pageable pageable);"
    out.println ""

    out.println "    List<$className> get${className}List(Specification<$className> spec, Sort sort);"
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
    def fields = calcFields(table)
    def count = 0
    fields.each() {
        if(contains(it.colName)) count ++
    }
    def javaName = javaName(className,false)

    out.println "package $packageName"
    out.println ""
    out.println "import $modelClassName;"
    out.println "import $repoClassName;"
    out.println "import $serviceClassName;"
    out.println "import org.springframework.data.domain.Page;"
    out.println "import org.springframework.data.domain.Pageable;"
    out.println "import org.springframework.data.domain.Sort;"
    out.println "import org.springframework.beans.factory.annotation.Autowired;"
    out.println "import org.springframework.data.jpa.domain.Specification;"
    out.println "import org.springframework.stereotype.Service;"

    out.println "import javax.validation.Valid;"
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
                "    public Page<${className}> get${className}Page(Specification<${className}> spec, Pageable pageable) {"
    if(count == 6){
        out.println  "        return ${javaName}Repository.findAllNormal(spec, pageable);"
    }else{
        out.println  "        return ${javaName}Repository.findAll(spec, pageable);"
    }
    out.println            "    }"
    out.println ""

    out.println "    @Override\n" +
            "    public List<${className}> get${className}List(Specification<${className}> spec, Sort sort) {"
    if(count == 6){
        out.println  "        return ${javaName}Repository.findAllNormal(spec, sort);"
    }else{
        out.println  "        return ${javaName}Repository.findAll(spec, sort);"
    }
    out.println            "    }"
    out.println ""

    out.println "    @Override\n" +
                "    public ${className} save${className}(${className} ${javaName}) {"
    if(count == 6){
        out.println            "        return ${javaName}Repository.saveNormal(${javaName});"
    }else{
        out.println            "        return ${javaName}Repository.save(${javaName});"
    }
    out.println "    }"
    out.println ""
    out.println "    @Override\n" +
                "    public ${className} update${className}(${className} ${javaName}) {"
    if(count == 6){
        out.println            "        ${javaName}.assertNotDelete();"
    }
    out.println "        return save${className}(${javaName});\n" +
                "    }"
    out.println ""
    out.println "    @Override\n" +
                "    public void delete${className}(Long id) {"
    if(count == 6){
        out.println            "        ${className} ${javaName} = ${javaName}Repository.getReferenceById(id);"
        out.println            "        ${javaName}Repository.deleteNormal(${javaName});"
    }else{
        out.println            "        ${javaName}Repository.deleteById(id);"
    }

    out.println "    }\n"
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


def calcFields(table) {
    def primaryKey = DasUtil.getPrimaryKey(table)
    def index = 0
    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDataType().getSpecification())
        index ++
        def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
        def colName = col.getName()
        def isId = primaryKey != null && DasUtil.containsName(colName, primaryKey.getColumnsRef())
        def comm = [
                colName : col.getName(),
                name    : javaName(col.getName(), false),
                type    : typeStr,
                commoent: col.getComment(),
                annos   : " @Column(name = \"" + col.getName() + "\")"+"\n"+"    @JsonProperty(value = \"" + col.getName() + "\",index = " + index + (isId?",access = JsonProperty.Access.READ_ONLY":"") + ")",
                isId : isId,
        ]
//        if ("id".equals(Case.LOWER.apply(col.getName())))
//            comm.annos += ["@Id"]
        fields += [comm]
    }
}

static boolean contains(String element){
    return "create_time" == element || "update_time" == element || "create_by" == element || "update_by" == element || "rank" == element  || "soft_delete" == element
}

static boolean contains6(String element){
    return contains(element) || "rank" == element  || "soft_delete" == element
}

static String getCleanComment(String comment) {
    return comment.replace("(NP)", "").replace("(E)", "").replace("(L)", "").replace("(B)", "")
}

static boolean tableIsNoPage(String comment) {
    return comment.contains("(NP)")
}

static boolean tableIsExport(String comment) {
    return comment.contains("(E)")
}

static boolean tableIsList(String comment) {
    return comment.contains("(L)")
}

static boolean tableHaveBase(String comment) {
    return comment.contains("(B)")
}

static String getProjectName(String projectStr){

    def s = "componentStore="
    def e = ")"
    def si = projectStr.indexOf(s)
    def ei = projectStr.indexOf(e,si + s.length())

    return projectStr.substring(si + s.length(),ei)
}