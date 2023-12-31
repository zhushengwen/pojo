import com.intellij.database.model.DasTable
import com.intellij.database.model.ObjectKind
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil


import java.io.*
import java.nio.file.Paths
import java.nio.file.Files
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
    if (!"System".equals(moduleName)) {
        moduleSub = "\\modules\\" + moduleName
    }
    def dir = getProjectName(PROJECT.toString()) + "\\src\\main\\java\\com\\jeiat\\itapi" + moduleSub + "\\model"
    def fields = calcFields(table)
    def tableName = table.getName()
    checkTableCommonent(table.getComment(), tableName)
    fields.each() {
        checkFieldCommonent(it.comment, it.colName, tableName)
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

    out.println "import com.jeiat.itapi.common.annotation.ByteMaxLength;"
    out.println "import com.jeiat.itapi.common.util.DictUtil;"
    out.println "import com.jeiat.itapi.common.component.EntityJoiner;"

    out.println "import com.jeiat.itapi.common.utils.AppUtils;"
    out.println "import io.swagger.annotations.ApiModel;"
    out.println "import io.swagger.annotations.ApiModelProperty;"

//    out.println "import javax.persistence.Entity;"
//    out.println "import javax.persistence.Table;"
    out.println ""
    out.println "import java.io.Serializable;"
    out.println ""

//  out.println "import lombok.Getter;"
//  out.println "import lombok.Setter;"
//  out.println "import lombok.ToString;"
    out.println "import lombok.Data;"
    out.println "import lombok.Getter;"
    out.println "import com.fasterxml.jackson.annotation.JsonAlias;"
    out.println "import com.fasterxml.jackson.annotation.JsonProperty;"

    out.println "import lombok.AllArgsConstructor;"
    out.println "import com.fasterxml.jackson.annotation.JsonIgnore;"
    out.println "import org.hibernate.annotations.NotFound;"
    out.println "import org.hibernate.annotations.NotFoundAction;"
    
    if (count == 4 || count == 5) {
        out.println "import com.jeiat.itapi.common.base.BaseEntity;"
    }
    if (count == 6) {
        out.println "import com.jeiat.itapi.common.base.CommonEntity;"
    }
    
    out.println "import lombok.EqualsAndHashCode;"
    out.println ""
    out.println "import javax.persistence.*;"
    out.println "import javax.validation.constraints.NotEmpty;"
    out.println "import javax.validation.constraints.NotNull;"
    out.println "import java.util.List;"
    out.println ""
    
    def baseName = className + "Base"
    def modPath = getProjectName(PROJECT.toString()) + "\\src\\main\\java\\com\\jeiat\\itapi\\modules\\" + moduleName
    new File(modPath).mkdir()
    def dir = modPath + "\\model"
    new File(dir).mkdir()
    new File(dir + "\\base").mkdir()
    def file = new File(dir + "\\base", baseName + ".java")
    
    def extendsStr = "implements Serializable"

    def ibaseWriter = null
    if (tableHaveBase(comment)) {
        if (!file.exists()) {
            PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))
            def baseExtendsStr = extendsStr
            if (count == 4 || count == 5) {
                baseExtendsStr = "extends BaseEntity implements "
            } else if (count == 6) {
                baseExtendsStr = "extends CommonEntity implements "
            }else{
                baseExtendsStr += ", "
            }

            baseExtendsStr += "I$baseName "

            printWriter.withPrintWriter { out2 ->
                out2.println "package ${packageName}.base;\n" +
                        "\n" +
                        "import com.jeiat.itapi.common.base.BaseEntity;\n" +
                        "import com.jeiat.itapi.common.base.CommonEntity;\n" +
                        "import ${packageName}.$className;\n" +
                        "import ${packageName}.base.meta.I${baseName};\n" +
                        "import lombok.Data;\n" +
                        "import lombok.EqualsAndHashCode;\n" +
                        "import javax.persistence.MappedSuperclass;\n" +
                        "\n" +
                        "import java.io.Serializable;\n" +
                        "import java.util.List;\n" +
                        "\n" +
                        "@Data\n" +
                        "@MappedSuperclass\n" +
                        "@EqualsAndHashCode(callSuper = false)\n" +
                        "public abstract class  $baseName $baseExtendsStr {\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "    public static class Joiner {\n" +
                        "        public static void joinBase(List<${className}> ignored) {\n" +
                        "\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n"
            }

        }
        //创建meta目录
        def baseDir = new File(dir + "\\base\\meta")
        baseDir.mkdir()
        def ibase = new File(baseDir, "I${baseName}.java")
        ibaseWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(ibase), "UTF-8"))
        writeLine(ibaseWriter, "package ${packageName}.base.meta;\n")
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

        if (isNotEmpty(it.rela)) {
            def typeName = getRelaTypeName(it.rela, moduleName, it.colName)
            def relaPackageName = getRelaPackageName(getRelaModeName(it.rela, moduleName), packageName, moduleName, typeName)
            if (isNotEmpty(relaPackageName)) {
                String string = "import $relaPackageName;"
                out.println string
                writeLine(ibaseWriter, string)
            }else{
                writeLine(ibaseWriter, "import ${packageName}.${typeName};")
            }
        }else if("".equals(it.rela)){
            def typeName = getRelaTypeName(it.rela, moduleName, it.colName)
            writeLine(ibaseWriter, "import ${packageName}.${typeName};")
        }

        if (isNotEmpty(it.join)) {
            String relaPackageName = it.join.split(/_/)[0]

            def typeName = javaName(it.join, true)
            relaPackageName = getRelaPackageName(relaPackageName, packageName, moduleName, typeName)
            if (isNotEmpty(relaPackageName)) {
                String string = "import $relaPackageName;"
                out.println string
                writeLine(ibaseWriter, string)
            }else{
                writeLine(ibaseWriter, "import ${packageName}.${typeName};")
            }
        }
    }

    if (types.contains("Date")) {
        out.println ""
        out.println "import java.util.Date;"
        out.println ""
        out.println "import com.fasterxml.jackson.annotation.JsonFormat;"
        if (isExport) {
            out.println "import com.alibaba.excel.annotation.format.DateTimeFormat;"
        }

        writeLine(ibaseWriter, "import java.util.Date;")
    }
    if (isExport) {
        out.println "import com.alibaba.excel.annotation.ExcelProperty;"
    }
    if (types.contains("Double")) {
        out.println "import com.fasterxml.jackson.databind.annotation.JsonSerialize;"
        out.println "import com.fasterxml.jackson.databind.annotation.JsonDeserialize;"
        out.println "import com.jeiat.itapi.common.utils.JsonDecimalFormat;"
        out.println "import com.jeiat.itapi.common.utils.TenThousandDeFormat;\n" +
                "import com.jeiat.itapi.common.utils.TenThousandFormat;"
    }
    if (types.contains("InputStream")) {
        out.println "import java.io.InputStream;"
    }

    out.println ""

    String warning = "/**\n" +
            " * Date " + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + "\n" +
            " * !!THIS FILE WILL BE REWRITE,SO IT'S READONLY,DO NOT CHANGE IT!!\n" +
            " * if you want to change it\n" +
            " * please extend it in it's baseclass ${className}Base\n" +
            " */"
    out.println warning
    out.println ""
//  out.println "@Setter"
//  out.println "@Getter"
//  out.println "@ToString"

    writeLine(ibaseWriter, "")
    writeLine(ibaseWriter, warning)
    writeLine(ibaseWriter, "")
    writeLine(ibaseWriter, "public interface I$baseName {")

    out.println "@Data"
    out.println "@Entity"
    out.println "@Table(name = \"" + tableName + "\")"
    out.println "@ApiModel(value = \"${getTableAnno(cleanComment)}模型\")"

    if (hasBase) {
        out.println "@EqualsAndHashCode(callSuper = false)"
        extendsStr = "extends ${className}Base"
    } else if (count == 4 || count == 5) {
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
            def isShow = false
            def isDate = it.dbType == "date"
            if (isNotEmpty(it.comment)) {

                if (it.comment.contains("(I)")) {
                    isIgnore = true
                }
                if (it.comment.contains("(NJA)")) {
                    isNja = true
                }
                if (it.comment.contains("(S)")) {
                    isShow = true
                }
            }

            // 输出注释
            if (it.comment.toString().contains("(T)")) {
                out.println "    @Transient"
            }
            it.comment = getCleanFieldComment(it.comment)
            out.println "    /**"
            out.println "     * ${it.comment.toString()}"
            out.println "     */"

            if (it.isId) out.println "    @Id"
            if (it.isId) out.println "    @GeneratedValue(strategy = GenerationType.IDENTITY)"

            out.println "    @Column(name = \"${it.colName}\")"
            def datePattern = isDate || it.comment.contains("日期") ? "yyyy-MM-dd" : "yyyy-MM-dd HH:mm:ss"
            def dpS = it.type == "Date" ? "(格式：$datePattern)" : ""
            out.println "    " + (isIgnore ? "//" : "") + "@JsonProperty(value = \"${it.colName}\", index = ${index}" + (it.isId || isShow ? ", access = JsonProperty.Access.READ_ONLY" : "") + ")"
            if (it.type == "Date") out.println "    @JsonFormat(pattern=\"$datePattern\",timezone = \"GMT+8\")"
            // 输出成员变量
            String defexp = ""
            String defStr = ""
            if(isNotEmpty(it.defexp)){
                String defval = ""
                if(it.type == "Long"){
                    defval = clearDefaultNum(it.defexp)
                    defexp = " = ${defval}L"
                }else if(it.type == "String"){
                    defval = it.defexp.replace("'","")
                    defexp = " = \"${defval}\""
                }else if(it.type == "Double"){
                    defval = clearDefaultNum(it.defexp)
                    defexp = " = ${defval}"
                }
                defStr = " [默认:" + defval + "]"
            }
            def required = ""
            if (!it.isId && it.notNull && !isShow) required = ", required = true"
            def noModify = ""
            if (!it.isId && (isIgnore || isNja || isShow)) noModify = " [只读]"

            out.println "    @ApiModelProperty(value = \"${it.comment}${dpS}${noModify}${defStr}\"${required})"
            if (tableIsExport(comment)) {
                String excelCommon = getClearExport(it)
                out.println "    @ExcelProperty(\"${excelCommon}\")"
                if ((it.type == "Date")) {
                    out.println "    @DateTimeFormat(\"yyyy-MM-dd\")"
                }
            }

            if (!it.isId && !isShow) {
                if (isIgnore)
                    out.println "    @JsonIgnore"
                else if (!isNja)
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
            if (!isShow) {
                if (it.type == "String") {
                    if (it.notNull) out.println "    @NotEmpty"
                    if (it.length != null && it.length > 0) out.println "    @ByteMaxLength(max = ${it.length})"
                } else {
                    if (it.notNull && !it.isId) out.println "    @NotNull"
                }
            }

            out.println "    private ${it.type} ${it.name}${defexp};"
            writeLine(ibaseWriter, "    ${it.type} get${javaName(it.name,true)}();")

            if (it.rela != null) {
                out.println ""
                out.println "    @JsonIgnore"
                out.println "    @OneToOne"
                out.println "    @NotFound(action = NotFoundAction.IGNORE)"
                out.println "    @JoinColumn(name = \"" + it.colName + "\", insertable = false, updatable = false)"
                def typeName = getRelaTypeName(it.rela, moduleName, it.colName)
                out.println "    private " + typeName + " " + getRelaVarName(it.colName) + ";"

                writeLine(ibaseWriter, "    " + typeName + " get" + javaName(getRelaVarName(it.colName),true) + "();")

                index = index + 1
                out.println ""
                String dictColName = it.colName
                dictColName = trimId(dictColName)
                javaDictName = javaName(dictColName, true)
                dictColName = dictColName + "_name"
                String javaDictColName = javaName(dictColName, true)
                String methodSignature = "public String get${javaDictColName}()"
                boolean genGetName = true
                if (hasBase && Files.readString(Paths.get(file.getAbsolutePath())).contains(methodSignature)) {
                    genGetName = false
                }
                if (genGetName) {
                    def cleanCommentName = getCleanComment(it)
                    if (tableIsExport(comment)) {
                        out.println "    @ExcelProperty(\"${cleanCommentName}\")"
                    }
                    out.println "    @JsonProperty(value = \"${dictColName}\", index = ${index}" + ", access = JsonProperty.Access.READ_ONLY" + ")"
                    out.println "    @ApiModelProperty(\"${cleanCommentName}\")"
                    out.println "    ${methodSignature} {\n" +
                            "        return AppUtils.ofNullable(${getRelaVarName(it.colName)}, ${typeName}::getName);\n" +
                            "    }"

                    writeLine(ibaseWriter, "    String get${javaDictColName}();")
                }

            }

            if (isNotEmpty(it.dict)) {
                index = index + 1
                out.println ""
                out.println "    @Transient"
                String dictColName = it.colName
                dictColName = trimId(dictColName)
                it.javaDictName = javaName(dictColName, true)
                dictColName = dictColName + "_name"
                String javaDictColName = javaName(dictColName, false)
                String cleanDict = getCleanComment(it)
                if (tableIsExport(comment)) {
                    out.println "    @ExcelProperty(\"${cleanDict}\")"
                }
                out.println "    @JsonProperty(value = \"${dictColName}\", index = ${index}" + ", access = JsonProperty.Access.READ_ONLY" + ")"
                out.println "    @ApiModelProperty(\"${cleanDict}\")"
                out.println "    private String $javaDictColName;"
                it.dictColName = dictColName

                writeLine(ibaseWriter, "    String get${javaName(javaDictColName,true)}();")
            }

            if (isNotEmpty(it.join)) {
                index = index + 1
                out.println ""
                out.println "    @Transient"
                out.println "    @JsonIgnore"
                out.println "    private ${javaName(it.join, true)} ${javaName(it.join, false)};"
                out.println ""
                String dictColName = it.colName
                dictColName = trimId(dictColName)
                javaDictName = javaName(dictColName, true)
                dictColName = dictColName + "_name"
                String cleanDict = getCleanComment(it)
                String javaDictColName = javaName(dictColName, true)
                String methodSignature = "public String get${javaDictColName}()"
                boolean genGetName = true
                if (hasBase && Files.readString(Paths.get(file.getAbsolutePath())).contains(methodSignature)) {
                    genGetName = false
                }
                if (genGetName) {

                    if (tableIsExport(comment)) {
                        out.println "    @ExcelProperty(\"${cleanDict}\")"
                    }
                    out.println "    @JsonProperty(value = \"${dictColName}\", index = ${index}" + ", access = JsonProperty.Access.READ_ONLY" + ")"
                    out.println "    @ApiModelProperty(\"${cleanDict}\")"
                    out.println "    public String get${javaDictColName}() {\n" +
                            "        return AppUtils.ofNullable(${javaName(it.join, false)}, ${javaName(it.join, true)}::getName);\n" +
                            "    }"

                    writeLine(ibaseWriter, "    String get${javaName(javaDictColName,true)}();")
                }
            }
        }
    }
    out.println ""
    String baseJoiner = hasBase ? "extends ${className}Base.Joiner " : ""
    out.println "    public static class Joiner ${baseJoiner}{\n" +
            "        public static void joinAll(List<${className}> list) {"
    out.println "            joinDicts(list);"
    if (hasBase) out.println "            joinBase(list);"
    fields.each() {
        if (isNotEmpty(it.join)) {
            String colName = it.colName
            colName = javaClassName(trimId(colName), true)
            out.println "            join${colName}(list);"
        }
    }
    out.println "        }"
    out.println ""
    out.println "        //dict"
    out.println "        @Getter\n" +
            "        @AllArgsConstructor\n" +
            "        public enum DictEnum {"
    boolean first = true
    fields.each() {
        if (isNotEmpty(it.dict)) {
            String firstLine = first ? "" : ","
            first = false
            out.print "${firstLine}\n" +
                    "            //${it.comment}\n" +
                    "            ${it.javaDictName}(\"${it.dict}\")"
        }
    }
    out.println "            ;"
    out.print "            private final String type;\n" +
            "            public static void join(List<${className}> list) {\n" +
            "                String[] types = new String[]{"
    fields.each() {
        if (isNotEmpty(it.dict)) {
            out.print "\n" +
                    "                        ${it.javaDictName}.getType(),"
        }
    }
    out.print "\n" +
            "                };\n" +
            "                new DictUtil.DictJoiner<>(list, types)"
    fields.each() {
        if (isNotEmpty(it.dict)) {
            out.print "\n" +
                    "                        .joinValue(${className}::get${javaName(it.name, true)}, ${className}::set${javaName(it.dictColName, true)})"
        }
    }
    out.println ";\n" +
            "            }\n" +
            "        }"
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
    out.println "        public static void joinDicts(List<${className}> list) {\n" +
            "            DictEnum.join(list);\n" +
            "        }"
    out.println ""
    fields.each() {
        if (isNotEmpty(it.join)) {
            String colName = it.colName
            colName = javaClassName(trimId(colName), true)
            out.println "        //关联表: ${it.join}"
            out.println "        public static void join${colName}(List<${className}> list) {\n" +
                    "            EntityJoiner.join(list, ${className}::get${javaClassName(it.colName, true)}, ${className}::get${javaClassName(it.join, true)});\n" +
                    "        }"
        }
    }
    out.println "    }"
    out.println "}"

    writeLine(ibaseWriter, "}")
    closeWrite(ibaseWriter)
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
        comment = removeRela(comment, rela)
        def isId = primaryKey != null && DasUtil.containsName(colName, primaryKey.getColumnsRef())
        def comm = [
                colName: col.getName(),
                name   : javaName(col.getName(), false),
                type   : typeStr,
                dbType : spec,
                comment: comment,
                index  : index,
                isId   : isId,
                notNull: col.isNotNull(),
                length : col.getDataType().getLength(),
                defexp : col.getDefault(),
                rela   : rela,
                dict   : getDict(comment),
                join   : getJoin(comment)
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
    return "    private static final long serialVersionUID =  " + Math.abs(new Random().nextLong()) + "L;"
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

static String getCleanFieldComment(String comment) {
    return comment.toString()
            .replace("(DE)", "")
            .replace("(E)", "")
            .replace("(T)", "")
            .replace("(I)", "")
            .replace("(BE)", "")
            .replace("(NJA)", "")
            .replace("(L)", "")
            .replace("(IN)", "")
            .replace("(S)", "")
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

static void checkTableCommonent(String comment, String table) {
    if (comment == null) throw new Exception("表${table}注释不能为空")
}

static void checkFieldCommonent(String comment, String field, String table) {
    if (comment == null) throw new Exception("表${table}字段${field}注释不能为空")
}

static String getProjectName(String projectStr) {
    def s = "componentStore="
    def e = ")"
    def si = projectStr.indexOf(s)
    def ei = projectStr.indexOf(e, si + s.length())
    def project = projectStr.substring(si + s.length(), ei)
    if (project.endsWith(".ipr")) {
        project = new File(project).getParent()
    }
    return project
}

static String getTableAnno(String anno) {
    for (int i = 0; i < anno.length(); i++) {
        if (!inChar(anno.charAt(i))) {
            return anno.substring(i)
        }
    }
    return anno
}

static boolean inChar(char b) {
    int s = (short) b

    return (s >= 48 && s <= 57) || s == 46
}

static String getDict(String common) {
    def pattern = ~/\((字典:\w+)?\)/
    def matcher = common =~ pattern
    if (matcher.find()) {
        if (matcher.groupCount() > 0) {
            def str = matcher.group(0)
            if (str.contains(":")) {
                return str.replace("(字典:", "").replace(")", "")
            } else {
                return ""
            }
        }
    }
    return null
}

static String getRela(String common) {
    def pattern = ~/\(R(:\w+)?\)/
    def matcher = common =~ pattern
    if (matcher.find()) {
        if (matcher.groupCount() > 0) {
            def str = matcher.group(0)
            if (str.contains(":")) {
                return str.replace("(R:", "").replace(")", "")
            } else {
                return ""
            }
        }
    }
    return null
}

static String removeRela(String common, String rela) {
    if (rela == null) return common
    if (rela.length() == 0) return common.replace("(R)", "")
    return common.replace("(R:" + rela + ")", "")
}

static String getRelaModeName(String rela, String thisMod) {
    rela = trimId(rela)
    if(rela.contains("_")){
        rela = rela.split(/_/)[0]
    }else{
        return ""
    }

    return rela.equals(thisMod) ? "" : rela
}
static String getRelaTypeName(String rela, String thisMod, String colName) {
    if(isNotEmpty(rela)){
        String modName = getRelaModeName(rela, thisMod)
        if(isNotEmpty(modName)){
            return javaName(trimId(rela), true)    
        }else{
            return javaName(rela, true)   
        }
    }

    return javaName(trimId(thisMod + "_" + colName), true)
}

static String getRelaVarName(String colName) {
    String name = colName.replace("_id", "")
    return javaName(name, false)
}

static String getRelaPackageName(String rela, String thisPackageName, String thisMod, String entityTypeName) {
    if (isNotEmpty(rela) && rela != thisMod) {
        return thisPackageName.replace(".$thisMod.", ".$rela.") + "." + entityTypeName
    }
    return null
}

static String trimId(String name) {
    if (name.endsWith("_id")) {
        return name.substring(0, name.length() - 3)
    }
    return name
}

static String getJoin(String common) {
    def pattern = ~/\((关联:\w+)?\)/
    def matcher = common =~ pattern
    if (matcher.find()) {
        if (matcher.groupCount() > 0) {
            def str = matcher.group(0)
            if (str.contains(":")) {
                return str.replace("(关联:", "").replace(")", "")
            } else {
                return ""
            }
        }
    }
    return null
}

static String clearJoin(String common, String name) {
    return common.replace("(关联:" + name + ")", "")
}

static String clearDict(String common, String name) {
    return common.replace("(字典:" + name + ")", "")
}
static String clearDefaultNum(String common) {
    return common.replace("'", "").replace("(", "").replace(")", "")
}

static String getClearExport(it) {
    String clean = it.comment
    if (isNotEmpty(it.rela))
        clean = removeRela(clean, it.rela)
    if (isNotEmpty(it.join))
        clean = clearJoin(clean, it.join)
    if (isNotEmpty(it.dict))
        clean = clearDict(clean, it.dict)
    return clean
}

static String getCleanComment(it){
    return getClearExport(it).replace("编号","").replace("ID","") +  "名称"
}

static void writeLine(PrintWriter out,String line){
    if(out != null){
        out.println line
    }
}

static void closeWrite(PrintWriter out){
    if(out != null){
        out.close()
    }
}