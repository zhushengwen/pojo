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
dir = ""
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

SELECTION.filter { it instanceof DasTable && it.getKind() == ObjectKind.TABLE }.each { generate(it) }

def generate(table) {
    def className = javaClassName(table.getName(), true)
    moduleName = table.getName().split(/_/)[0]
    def moduleSub = ""
    if(!"System".equals(moduleName)){
        moduleSub = "\\modules\\" + moduleName
    }
    def dir = getProjectName(PROJECT.toString()) + "\\src\\main\\java\\com\\jeiat\\itapi" + moduleSub + "\\model"
    def fields = calcFields(table)
    def tableName = table.getName()
    checkTableCommonent(table.getComment(), tableName)
    fields.each() {
        checkFieldCommonent(it.comment,it.colName,tableName)
    }
    packageName = getPackageName(dir)
    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(dir, className + ".java")), "UTF-8"))
    printWriter.withPrintWriter { out -> generate(out, className, fields, table) }

//    new File(dir, className + ".java").withPrintWriter { out -> generate(out, className, fields,table) }
}

// 获取包所在文件夹路径
static def getPackageName(dir) {
    return dir.toString().replaceAll("\\\\", ".").replaceAll("/", ".").replaceAll("^.*src(\\.main\\.java\\.)?", "")
}

def generate(out, className, fields, table) {
    def tableName = table.getName()
    def moduleName = tableName.split(/_/)[0]
    def comment = table.getComment()
    def count = 0
    fields.each() {
        if (contains(it.colName)) count++
    }
    def cleanComment = getCleanTableComment(comment)
    def isExport = tableIsExport(comment)
    out.println "package $packageName;"
    out.println ""
    out.println "import javax.persistence.*;"
    out.println "import com.jeiat.itapi.annotation.ByteMaxLength;"
    out.println "import javax.validation.constraints.NotEmpty;"
    out.println "import javax.validation.constraints.NotNull;"
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
    out.println "import org.hibernate.annotations.NotFound;"
    out.println "import org.hibernate.annotations.NotFoundAction;"
    if (count == 4) {
        out.println "import com.jeiat.itapi.base.BaseEntity;"
        out.println "import lombok.EqualsAndHashCode;"
    }
    if (count == 6) {
        out.println "import com.jeiat.itapi.base.CommonEntity;"
        out.println "import lombok.EqualsAndHashCode;"
    }

    def baseName = className + "Base"
    def dir = getProjectName(PROJECT.toString()) + "\\src\\main\\java\\com\\jeiat\\itapi\\modules\\" + moduleName + "\\model"
    def file = new File(dir + "\\base", baseName + ".java")

    if (tableHaveBase(comment)) {
        if (!file.exists()) {
            PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))
            printWriter.withPrintWriter { out2 ->
                        out2.println "package com.jeiat.itapi.modules.${moduleName}.model.base;\n" +
                            "\n" +
                            "import com.jeiat.itapi.base.CommonEntity;\n" +
                            "import lombok.Data;\n" +
                            "import lombok.EqualsAndHashCode;\n" +
                            "\n" +
                            "\n" +
                            "@Data\n" +
                            "@EqualsAndHashCode(callSuper = false)\n" +
                            "public abstract class  $baseName extends CommonEntity  {\n" +
                            "\n" +
                            "\n" +
                            "\n" +
                            "}\n"

            }
        }
    }

    def hasBase = false
    if (file.exists()) {
        hasBase = true
        out.println "import ${packageName}.base.${className}Base;"
    }
    Set types = new HashSet()

    fields.each() {
        if (count != 4 || !contains(it.colName)) {
            types.add(it.type)
        } else if (count != 6 || !contains6(it.colName)) {
            types.add(it.type)
        }

        if(isNotEmpty(it.rela)){
            def relaPackageName = getRelaPackageName(it.rela, packageName, moduleName, getRelaModeName(it.rela, moduleName, it.colName))
            if(isNotEmpty(relaPackageName)){
                out.println "import $relaPackageName;"
            }
        }

    }

    if (types.contains("Date")) {
        out.println "import java.util.Date;"
        out.println "import com.fasterxml.jackson.annotation.JsonFormat;"
        if (isExport) {
            out.println "import com.alibaba.excel.annotation.format.DateTimeFormat;"
        }
    }
    if (isExport) {
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
    out.println "@Table(name =\"" + tableName + "\")"
    out.println "@ApiModel(value =\"${getTableAnno(cleanComment)}模型\")"
    def extendsStr = "implements Serializable"
    if (hasBase) {
        out.println "@EqualsAndHashCode(callSuper = false)"
        extendsStr = "extends ${className}Base"
    } else if (count == 4) {
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
            def isIgnore = false
            def isNja = false
            if (isNotEmpty(it.comment)) {
                it.comment = it.comment.replace("(L)", "")
                it.comment = it.comment.replace("(E)", "")
                it.comment = it.comment.replace("(DE)", "")
                it.comment = it.comment.replace("(BE)", "")
            }
            if (isNotEmpty(it.comment)) {

                if (it.comment.toString().contains("(I)")) {
                    isIgnore = true
                    it.comment = it.comment.replace("(I)", "")
                }


                if (it.comment.toString().contains("(NJA)")) {
                    isNja = true
                    it.comment = it.comment.replace("(NJA)", "")
                }
            }

            // 输出注释
            if (isNotEmpty(it.comment)) {
                if (it.comment.toString().contains("(T)")) {
                    out.println "    @Transient"
                    it.comment = it.comment.replace("(T)", "")
                }
                out.println "    /**"
                out.println "     * ${it.comment.toString()}"
                out.println "     */"
            }


            if (it.isId) out.println "    @Id"
            if (it.isId) out.println "    @GeneratedValue(strategy = GenerationType.IDENTITY)"

            out.println "    @Column(name = \"${it.colName}\")"
            out.println "    " + (isIgnore ? "//" : "") + "@JsonProperty(value = \"${it.colName}\", index = ${index}" + (it.isId ? ", access = JsonProperty.Access.READ_ONLY" : "") + ")"
            def datePattern = it.comment.contains("日期") ? "yyyy-MM-dd" : "yyyy-MM-dd HH:mm:ss"
            if (it.type == "Date") out.println "    @JsonFormat(pattern=\"$datePattern\",timezone = \"GMT+8\")"
            // 输出成员变量
            if (isNotEmpty(it.comment)) {
                def required = ""
                if(!it.isId && it.notNull) required = ", required = true"
                def noModify = ""
                if(!it.isId && (isIgnore || isNja)) noModify = " [只读]"
                out.println "    @ApiModelProperty(value=\"${it.comment.toString()}${noModify}\"${required})"
                if (tableIsExport(comment)) {
                    out.println "    @ExcelProperty(\"${it.comment.toString()}\")"
                    if ((it.type == "Date")) {
                        out.println "    @DateTimeFormat(\"yyyy-MM-dd\")"
                    }
                }
            }
            if (!it.isId) {
                if (isIgnore)
                    out.println "    @JsonIgnore"
                else if(!isNja)
                    out.println "    @JsonAlias"
            }
            if (it.type == "Double") {
                if (isNotEmpty(it.comment)) {
                    if (it.comment.toString().contains("万元"))
                        out.println "    @JsonSerialize(using = TenThousandFormat.class)\n" +
                                "    @JsonDeserialize(using = TenThousandDeFormat.class)"
                } else {
                    out.println "    @JsonSerialize(using = JsonDecimalFormat.class)"
                }
            }

            if (it.type == "String") {
                if (it.notNull) out.println "    @NotEmpty"
                if (it.length != null && it.length > 0) out.println "    @ByteMaxLength(max = ${it.length})"
            }else{
                if (it.notNull && !it.isId) out.println "    @NotNull"
            }

            out.println "    private ${it.type} ${it.name};"

            if (it.rela) {
                out.println ""
                out.println "    @JsonIgnore"
                out.println "    @OneToOne"
                out.println "    @NotFound(action = NotFoundAction.IGNORE)"
                out.println "    @JoinColumn(name = \"" + it.colName + "\", insertable = false, updatable = false)"
                def typeName = getRelaModeName(it.rela, moduleName, it.colName)
                out.println "    private " + typeName + " " + getRelaVarName(typeName) + ";"
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
        def comment = col.getComment()
        def rela = getRela(comment)
        comment = removeRela(comment,rela)
        def isId = primaryKey != null && DasUtil.containsName(colName, primaryKey.getColumnsRef())
        def comm = [
                colName : col.getName(),
                name    : javaName(col.getName(), false),
                type    : typeStr,
                comment: comment,
                index   : index,
                isId    : isId,
                notNull : col.isNotNull(),
                length  : col.getDataType().getLength(),
                rela    : rela
        ]
//        if ("id".equals(Case.LOWER.apply(col.getName())))
//            comm.annos += ["@Id"]
        fields += [comm]
    }
}

// 处理类名（这里是因为我的表都是以t_命名的，所以需要处理去掉生成类名时的开头的T，
// 如果你不需要那么请查找用到了 javaClassName这个方法的地方修改为 javaName 即可）
static String javaClassName(str, capitalize) {
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
            .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    // 去除开头的T  http://developer.51cto.com/art/200906/129168.htm
    // s = s[1..s.size() - 1]
    capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}

static String javaName(str, capitalize) {
//    def s = str.split(/(?<=[^\p{IsLetter}])/).collect { Case.LOWER.apply(it).capitalize() }
//            .join("").replaceAll(/[^\p{javaJavaIdentifierPart}]/, "_")
//    capitalize || s.length() == 1? s : Case.LOWER.apply(s[0]) + s[1..-1]
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
            .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}

static boolean isNotEmpty(content) {
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

static String getCleanTableComment(String comment) {
    return comment.replace("(NP)", "").replace("(E)", "").replace("(L)", "").replace("(B)", "").replace("(M)", "")
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

static boolean fieldIsNoChange(String comment) {
    return comment.contains("(NJA)")
}
static void checkTableCommonent(String comment,String table){
    if(comment == null )throw new Exception("表${table}注释不能为空")
}
static void checkFieldCommonent(String comment,String field,String table){
    if(comment == null )throw new Exception("表${table}字段${field}注释不能为空")
}
static String getProjectName(String projectStr){
    def s = "componentStore="
    def e = ")"
    def si = projectStr.indexOf(s)
    def ei = projectStr.indexOf(e,si + s.length())
    def project = projectStr.substring(si + s.length(),ei)
    if(project.endsWith(".ipr")){
        project = new File(project).getParent()
    }
    return project
}

static String getTableAnno(String anno){
    for (int i= 0;i<anno.length();i++){
        if(!inChar(anno.charAt(i))){
            return anno.substring(i)
        }
    }
    return anno
}
static boolean  inChar(char b){
    int s = (short)b

    return (s >= 48 && s <= 57) || s == 46
}

static String getRela(String common){
    def pattern = ~/\(R(:\w+)?\)/
    def matcher = common =~ pattern
    if(matcher.find()){
        if(matcher.groupCount()>0){
            def str = matcher.group(0)
            if(str.contains(":")){
                return str.replace("(R:","").replace(")","")
            }else{
                return ""
            }
        }
    }
    return null
}
static String removeRela(String common,String rela){
    if(rela == null)return common
    if(rela.length() == 0)return common.replace("(R)","")
    return common.replace("(R:" + rela + ")","")
}
static String getRelaModeName(String rela,String thisMod,String colName){
    String name = colName.replace("_id", "")
    return javaName(( rela.length() == 0 ? thisMod : rela ) + javaName(name,true),true)
}
static String getRelaVarName(String name){
    return javaName(name,false)
}
static String getRelaPackageName(String rela,String thisPackageName,String thisMod, String entityTypeName){
    if(isNotEmpty(rela)){
        return thisPackageName.replace(".$thisMod.",".$rela.") + "." + entityTypeName
    }
    return null
}