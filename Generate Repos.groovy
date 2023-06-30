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
typeMapping = [
        (~/(?i)tinyint|smallint|mediumint/)      : "Long",
        (~/(?i)int/)                             : "Long",
        (~/(?i)bool|bit/)                        : "Boolean",
        (~/(?i)float|double|decimal|real/)       : "Double",
        (~/(?i)datetime|timestamp|date|time/)    : "Date",
        (~/(?i)blob|binary|bfile|clob|raw|image/): "InputStream",
        (~/(?i)/)                                : "String"
]

//FILES.chooseDirectoryAndSave("Choose repo directory", "Choose where to store generated files") { dir ->
//    SELECTION.filter { it instanceof DasTable && it.getKind() == ObjectKind.TABLE }.each { generate(it, dir) }
//}
dir = ""
SELECTION.filter { it instanceof DasTable && it.getKind() == ObjectKind.TABLE }.each { generate(it, dir) }

def generate(table, dir) {
    moduleName = table.getName().split(/_/)[0]
    dir = getProjectName(PROJECT.toString()) + "\\src\\main\\java\\com\\jeiat\\itapi\\modules\\"+moduleName+"\\repo"
    def className = javaClassName(table.getName(), true)
    packageName = getPackageName(dir)
    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(dir, className + "Repository.java")), "UTF-8"))
    printWriter.withPrintWriter { out -> generate(out, className, table) }

//    new File(dir, className + ".java").withPrintWriter { out -> generate(out, className, fields,table) }
}

// 获取包所在文件夹路径
def getPackageName(dir) {
    return dir.toString().replaceAll("\\\\", ".").replaceAll("/", ".").replaceAll("^.*src(\\.main\\.java\\.)?", "") + ";"
}

def generate(out, className, table) {
    def fields = calcFields(table)
    def count = 0
    fields.each() {
        if(contains(it.colName)) count ++
    }
    def s = packageName.split(/\./)
    //s.each { ele -> out.println ele }
    def modelClassName = s[0..-2].join(".") + ".model." + className
    out.println "package $packageName"
    out.println ""
    out.println "import org.springframework.stereotype.Repository;"
    if(count == 6){
        out.println "import com.jeiat.itapi.base.ICommonRepository;"
    }else{
        out.println "import org.springframework.data.jpa.repository.JpaRepository;"
        out.println "import org.springframework.data.jpa.repository.JpaSpecificationExecutor;"
    }

    out.println "import $modelClassName;"

    out.println ""
    out.println "/**\n" +
            " * Date " + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + " \n" +
            " */"
    out.println ""
    out.println "@Repository"
    if(count == 6){
        out.println "public interface ${className}Repository extends ICommonRepository<$className, Long> {"
    }else{
        out.println "public interface ${className}Repository extends JpaSpecificationExecutor<$className>, JpaRepository<$className, Long> {"
    }

    out.println ""
    out.println ""
    out.println "}"
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

static boolean contains(String element){
    return "create_time" == element || "update_time" == element || "create_by" == element || "update_by" == element || "rank" == element  || "soft_delete" == element
}

static boolean contains6(String element){
    return contains(element) || "rank" == element  || "soft_delete" == element
}

static String getProjectName(String projectStr){

    def s = "componentStore="
    def e = ")"
    def si = projectStr.indexOf(s)
    def ei = projectStr.indexOf(e,si + s.length())

    return projectStr.substring(si + s.length(),ei)
}