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


//FILES.chooseDirectoryAndSave("Choose model directory", "Choose where to store generated files") { dir ->
//    SELECTION.filter { it instanceof DasTable && it.getKind() == ObjectKind.TABLE }.each { generate(it, dir) }
//}
def dir = "C:\\soft\\java\\code\\src\\main\\java\\com\\jeiat\\itapi\\modules\\" + moduleName + "\\model"
SELECTION.filter { it instanceof DasTable && it.getKind() == ObjectKind.TABLE }.each { generate(it) }

def generate(table) {
    def className = javaClassName(table.getName(), true)
    moduleName = table.getName().split(/_/)[0]
    dir = "C:\\soft\\java\\code\\src\\main\\java\\com\\jeiat\\itapi\\modules\\" + moduleName + "\\model"
    def fields = calcFields(table)
    packageName = getPackageName(dir)
    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(dir, className + ".java")), "UTF-8"))
    printWriter.withPrintWriter { out -> generate(out, className, fields, table) }

//    new File(dir, className + ".java").withPrintWriter { out -> generate(out, className, fields,table) }
}

// 获取包所在文件夹路径
static def getPackageName(dir) {
    return dir.toString().replaceAll("\\\\", ".").replaceAll("/", ".").replaceAll("^.*src(\\.main\\.java\\.)?", "") + ";"
}

def generate(out, className, fields, table) {
    def count = 0
    fields.each() {
        if (contains(it.colName)) count++
    }
    def comment = table.getComment()
    def cleanComment = getCleanComment(comment)
    def isExport = tableIsExport(comment)
    out.println "package $packageName"
    out.println ""
    out.println "import javax.persistence.*;"
    out.println "import com.jeiat.itapi.annotation.ByteMaxLength;"
    out.println "import javax.validation.constraints.NotEmpty;"
//    out.println "import javax.persistence.Entity;"
//    out.println "import javax.persistence.Table;"
    out.println "import java.io.Serializable;"
//  out.println "import lombok.Getter;"
//  out.println "import lombok.Setter;"
//  out.println "import lombok.ToString;"
    out.println "import lombok.Data;"
    out.println "import com.fasterxml.jackson.annotation.JsonAlias;"
    out.println "import com.fasterxml.jackson.annotation.JsonProperty;"
    out.println "import io.swagger.annotations.ApiModel;"
    out.println "import io.swagger.annotations.ApiModelProperty;"
    out.println "import com.fasterxml.jackson.annotation.JsonIgnore;"
    if (count == 4) {
        out.println "import com.jeiat.itapi.base.BaseEntity;"
        out.println "import lombok.EqualsAndHashCode;"
    }
    if (count == 6) {
        out.println "import com.jeiat.itapi.base.CommonEntity;"
        out.println "import lombok.EqualsAndHashCode;"
    }
    def file = new File(dir + "\\base", className + "Base" + ".java")
    def hasBase = false
    if(file.exists()){
        hasBase = true
        out.println "import ${packageName}.base.${className}Base"
    }
    Set types = new HashSet()

    fields.each() {
        if (count != 4 || !contains(it.colName)) {
            types.add(it.type)
        } else if (count != 6 || !contains6(it.colName)) {
            types.add(it.type)
        }
    }

    if (types.contains("Date")) {
        out.println "import java.util.Date;"
        out.println "import com.fasterxml.jackson.annotation.JsonFormat;"
        if (isExport) {
            out.println "import com.alibaba.excel.annotation.format.DateTimeFormat;"
        }
    }
    if(isExport){
        out.println "import com.alibaba.excel.annotation.ExcelProperty;"
    }
    if (types.contains("Double")) {
        out.println "import com.fasterxml.jackson.databind.annotation.JsonSerialize;"
        out.println "import com.fasterxml.jackson.databind.annotation.JsonDeserialize;"
        out.println "import com.jeiat.itapi.utils.JsonDecimalFormat;"
        out.println "import com.jeiat.itapi.utils.TenThousandDeFormat;\n" +
                "import com.jeiat.itapi.utils.TenThousandFormat;"
    }
    if (types.contains("InputStream")) {
        out.println "import java.io.InputStream;"
    }
    out.println ""
//  out.println "/**\n" +
//          " * @Description  \n" +
//          " * @Date " + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + " \n" +
//          " */"
    out.println ""
//  out.println "@Setter"
//  out.println "@Getter"
//  out.println "@ToString"


    out.println "@Data"
    out.println "@Entity"
    out.println "@Table(name =\"" + table.getName() + "\")"
    out.println "@ApiModel(value =\"${cleanComment}模型\")"
    def extendsStr = "implements Serializable"
    if(hasBase){
        out.println "@EqualsAndHashCode(callSuper = false)"
        extendsStr = "extends ${className}Base"
    }else if (count == 4) {
        out.println "@EqualsAndHashCode(callSuper = false)"
        extendsStr = "extends BaseEntity "
    } else if (count == 6) {
        out.println "@EqualsAndHashCode(callSuper = false)"
        extendsStr = "extends CommonEntity "
    }


    out.println "public class $className ${extendsStr} {"
    out.println ""
    out.println genSerialID()
    index = 0
    fields.each() {
        if ((count != 4 || !contains(it.colName)) && (count != 6 || !contains6(it.colName))) {
            index = index + 1
            out.println ""
            def isRela = false
            def isIgnore = false
            if (isNotEmpty(it.commoent)) {
                it.commoent =  it.commoent.replace("(E)", "")
                if (it.commoent.toString().contains("(R)")) {
                    isRela = true
                    it.commoent = it.commoent.replace("(R)","")
                }
            }
            if (isNotEmpty(it.commoent)) {

                if (it.commoent.toString().contains("(I)"))
                {
                    isIgnore = true
                    it.commoent = it.commoent.replace("(I)","")
                }

            }

            // 输出注释
            if (isNotEmpty(it.commoent)) {
                if (it.commoent.toString().contains("(T)")){
                    out.println "    @Transient"
                    it.commoent = it.commoent.replace("(T)","")
                }
                out.println "    /**"
                out.println "     * ${it.commoent.toString()}"
                out.println "     */"
            }



            if (it.isId) out.println "    @Id"
            if (it.isId) out.println "    @GeneratedValue(strategy = GenerationType.IDENTITY)"

            out.println "    @Column(name = \"${it.colName}\")"
            out.println "    "+(isIgnore?"//":"")+"@JsonProperty(value = \"${it.colName}\", index = ${it.index}" + (it.isId ? " ,access = JsonProperty.Access.READ_ONLY" : "") + ")"

            if (it.type == "Date") out.println "    @JsonFormat(pattern=\"yyyy-MM-dd\",timezone = \"GMT+8\")"
            // 输出成员变量
            if (isNotEmpty(it.commoent)) {
                out.println "    @ApiModelProperty(value=\"${it.commoent.toString()}\")"
                if (tableIsExport(comment)) {
                    out.println "    @ExcelProperty(\"${it.commoent.toString()}\")"
                    if ((it.type == "Date")) {
                        out.println "    @DateTimeFormat(\"yyyy-MM-dd\")"
                    }
                }
            }
            if (!it.isId) {
                if(isIgnore)
                out.println "    @JsonIgnore"
                else
                out.println "    @JsonAlias"
            }
            if (it.type == "Double") {
                if (isNotEmpty(it.commoent)) {
                    if (it.commoent.toString().contains("万元"))
                        out.println "    @JsonSerialize(using = TenThousandFormat.class)\n" +
                                "    @JsonDeserialize(using = TenThousandDeFormat.class)"
                } else {
                    out.println "    @JsonSerialize(using = JsonDecimalFormat.class)"
                }
            }

            if(it.type == "String"){
               if(it.notNull) out.println "    @NotEmpty"
               if(it.length != null && it.length>0) out.println "    @ByteMaxLength(max = ${it.length})"
            }

            out.println "    private ${it.type} ${it.name};"

            if (isRela){
                out.println ""
                out.println "    @JsonIgnore"
                out.println "    @OneToOne"
                out.println "    @JoinColumn(name = \""+ it.colName +"\", insertable = false, updatable = false)"
                def relaName =   it.colName.replace("_id","")
                out.println "    private "+ javaName(moduleName + "_" + relaName,true) +" "+ javaName(relaName,false) +";"
            }

        }
    }

    // 输出get/set方法
//    fields.each() {
//        out.println ""
//        out.println "\tpublic ${it.type} get${it.name.capitalize()}() {"
//        out.println "\t\treturn this.${it.name};"
//        out.println "\t}"
//        out.println ""
//
//        out.println "\tpublic void set${it.name.capitalize()}(${it.type} ${it.name}) {"
//        out.println "\t\tthis.${it.name} = ${it.name};"
//        out.println "\t}"
//    }
    out.println ""
    out.println "}"
}

def calcFields(table) {
    def primaryKey = DasUtil.getPrimaryKey(table)
    def index = 0
    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDataType().getSpecification())
        index++
        def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
        def colName = col.getName()
        def isId = primaryKey != null && DasUtil.containsName(colName, primaryKey.getColumnsRef())
        def comm = [
                colName : col.getName(),
                name    : javaName(col.getName(), false),
                type    : typeStr,
                commoent: col.getComment(),
                index   : index,
                isId    : isId,
                notNull : col.isNotNull(),
                length  : col.getDataType().getLength()
        ]
//        if ("id".equals(Case.LOWER.apply(col.getName())))
//            comm.annos += ["@Id"]
        fields += [comm]
    }
}

// 处理类名（这里是因为我的表都是以t_命名的，所以需要处理去掉生成类名时的开头的T，
// 如果你不需要那么请查找用到了 javaClassName这个方法的地方修改为 javaName 即可）
def javaClassName(str, capitalize) {
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
            .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    // 去除开头的T  http://developer.51cto.com/art/200906/129168.htm
    // s = s[1..s.size() - 1]
    capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}

def javaName(str, capitalize) {
//    def s = str.split(/(?<=[^\p{IsLetter}])/).collect { Case.LOWER.apply(it).capitalize() }
//            .join("").replaceAll(/[^\p{javaJavaIdentifierPart}]/, "_")
//    capitalize || s.length() == 1? s : Case.LOWER.apply(s[0]) + s[1..-1]
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
            .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}

def isNotEmpty(content) {
    return content != null && content.toString().trim().length() > 0
}

static String changeStyle(String str, boolean toCamel) {
    if (!str || str.size() <= 1)
        return str

    if (toCamel) {
        String r = str.toLowerCase().split('_').collect { cc -> Case.LOWER.apply(cc).capitalize() }.join('')
        return r[0].toLowerCase() + r[1..-1]
    } else {
        str = str[0].toLowerCase() + str[1..-1]
        return str.collect { cc -> ((char) cc).isUpperCase() ? '_' + cc.toLowerCase() : cc }.join('')
    }
}

static String genSerialID() {
    //return "\tprivate static final long serialVersionUID =  " + Math.abs(new Random().nextLong()) + "L;"
    return "    private static final long serialVersionUID =  1L;"
}

static boolean contains(String element) {
    return "create_time" == element || "update_time" == element || "create_by" == element || "update_by" == element || "rank" == element || "soft_delete" == element
}

static boolean contains6(String element) {
    return contains(element) || "rank" == element || "soft_delete" == element
}

static String getCleanComment(String comment) {
    return comment.replace("(NP)", "").replace("(E)", "").replace("(L)", "")
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