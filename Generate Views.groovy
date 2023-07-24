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
//FILES.chooseDirectoryAndSave("Choose rest directory", "Choose where to store generated files") { dir ->
//    SELECTION.filter { it instanceof DasTable && it.getKind() == ObjectKind.TABLE }.each { generate(it, dir) }
//}
dir = ""
SELECTION.filter { it instanceof DasTable && it.getKind() == ObjectKind.TABLE }.each { generate(it, dir) }

def generate(table, dir) {
    moduleName = table.getName().split(/_/)[0]
    dir = getProjectName(PROJECT.toString()) + "\\src\\main\\java\\com\\jeiat\\itapi\\modules\\" + moduleName + "\\rest"
    def className = javaClassName(table.getName(), true)
    packageName = getPackageName(dir)
    def s = packageName.split(/\./)
    modelClassName = s[0..-2].join(".") + ".model." + className
    repoClassName = s[0..-2].join(".") + ".repo." + className + "Repository"
    serviceClassName = s[0..-2].join(".") + ".service." + className + "Service"

    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(dir, className + "Controller.java")), "UTF-8"))
    printWriter.withPrintWriter { out -> generate(out, className, table) }

    //s.each { ele -> out.println ele }
}

// 获取包所在文件夹路径
def getPackageName(dir) {
    return dir.toString().replaceAll("\\\\", ".").replaceAll("/", ".").replaceAll("^.*src(\\.main\\.java\\.)?", "") + ";"
}

def generate(out, className, table) {
    def fields = calcFields(table)
    def count = 0
    fields.each() {
        if (contains(it.colName)) count++
    }
    def rankField = containRank(fields) ? "rank" : "id"
    def model = table.getName()
    def javaName = javaName(className, false)
    def comment = table.getComment() as String
    def cleanComment =  getCleanComment(comment)
    def modelAnno = isNotEmpty(comment) ? cleanComment : "模型"
    def anno = getTableAnno(modelAnno)
    out.println "package $packageName"
    out.println ""
    out.println "import com.jeiat.itapi.common.Result;\n" +
            "import com.jeiat.itapi.modules.logging.aop.log.Log;\n" +
            "import com.jeiat.itapi.modules." + moduleName + ".model.${className};\n" +
            "import com.jeiat.itapi.modules." + moduleName + ".service.${className}Service;\n" +
            "import com.jeiat.itapi.utils.AppUtils;\n" +
            "import io.swagger.annotations.Api;\n" +
            "import io.swagger.annotations.ApiImplicitParam;\n" +
            "import io.swagger.annotations.ApiImplicitParams;\n" +
            "import io.swagger.annotations.ApiOperation;\n" +
            "import net.kaczmarzyk.spring.data.jpa.domain.Equal;\n" +
            "import net.kaczmarzyk.spring.data.jpa.domain.GreaterThanOrEqual;\n" +
            "import net.kaczmarzyk.spring.data.jpa.domain.LessThanOrEqual;\n" +
            "import net.kaczmarzyk.spring.data.jpa.domain.Like;\n" +
            "import net.kaczmarzyk.spring.data.jpa.web.annotation.And;\n" +
            "import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;\n" +
            "import org.springframework.beans.factory.annotation.Autowired;\n" +
            "import org.springframework.data.domain.Page;\n" +
            "import org.springframework.data.domain.Pageable;\n" +
            "import org.springframework.data.domain.Sort;\n" +
            "import org.springframework.data.jpa.domain.Specification;\n" +
            "import org.springframework.data.web.SortDefault;\n" +
            "import org.springframework.security.access.prepost.PreAuthorize;\n" +
            "import com.fasterxml.jackson.databind.node.ObjectNode;\n" +
            "import springfox.documentation.annotations.ApiIgnore;\n" +
            "import org.springframework.web.bind.annotation.*;"

    if(tableIsExport(comment)){
        out.println "import javax.servlet.http.HttpServletResponse;"
        out.println "import com.jeiat.itapi.common.ExcelUtil;"
    }

    out.println "import javax.transaction.Transactional;"
    out.println "import javax.validation.Valid;"
    out.println "import java.util.List;"


    out.println ""
    out.println "/**\n" +
            " * Date " + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + " \n" +
            " */"
    out.println ""
    out.println "@Api(tags = \"${modelAnno}接口\")\n" +
            "@RestController\n" +
            "@RequestMapping(\"/api/" + moduleName + "/${model}s\")\n" +
            "public class ${className}Controller {\n" +
            "\n" +
            "    @Autowired\n" +
            "    ${className}Service ${javaName}Service;\n"

    def params = []
    def paramsPage = []
    def specs = []
    params.add("            @ApiImplicitParam(name = \"id\", value = \"编号\", dataType = \"integer\"),")
    specs.add("            @Spec(path = \"id\", params = \"id\", spec = Equal.class),")
    fields.each() {
        if (isNotEmpty(it.commoent)) {
            def isEqal = it.commoent.toString().contains("(E)")
            def isDefault = it.commoent.toString().contains("(DE)")
            def isBetween = it.commoent.toString().contains("(BE)")
            isEqal = isEqal || isDefault
            def isLike = it.commoent.toString().contains("(L)")
            def colComment = it.commoent.toString().replace("(E)","").replace("(L)","").replace("(DE)","").replace("(BE)","").replace("(R)","")
            def defaultRequire = ""
            def defaultValue = ""
            if(isDefault){
                defaultRequire = ", required = true"
                defaultValue = ", defaultVal = \"0\""
            }
            if (isEqal) {
                params.add("            @ApiImplicitParam(name = \"${it.colName}\", value = \"${colComment}\"${defaultRequire}),")
                specs.add("            @Spec(path = \"${it.name}\", params = \"${it.colName}\", spec = Equal.class${defaultValue}),")
            }
            if(isBetween){
                params.add("            @ApiImplicitParam(name = \"start_${it.colName}\", value = \"开始${colComment}\"),")
                params.add("            @ApiImplicitParam(name = \"end_${it.colName}\", value = \"结束${colComment}\"),")
                specs.add("            @Spec(path = \"${it.name}\", params = \"start_${it.colName}\", spec = GreaterThanOrEqual.class),")
                specs.add("            @Spec(path = \"${it.name}\", params = \"end_${it.colName}\", spec = LessThanOrEqual.class),")
            }
            if (isLike) {
                params.add("            @ApiImplicitParam(name = \"${it.colName}\", value = \"${colComment}\"),")
                specs.add("            @Spec(path = \"${it.name}\", params = \"${it.colName}\", spec = Like.class),")
            }
        }

    }
    def listComment = "分页"
    def method = "page"
    def isList = tableIsList(comment)
    if(isList){
       comment = comment.replace("(NP)","")
    }
    if (tableIsNoPage(comment)) {
        method = "list"
        listComment = "列表"
    }else{
        paramsPage.add("            @ApiImplicitParam(name = \"page\", value = \"页码：起始值1\", dataType = \"integer\"),")
        paramsPage.add("            @ApiImplicitParam(name = \"limit\", value = \"返回条数：默认20\", dataType = \"integer\"),")
    }


    out.println "    @ApiOperation(\"${anno}${listComment}\")\n" +
            "    @GetMapping\n" +
            "    @PreAuthorize(\"@el.check(0)\")\n" +
            "    @ApiImplicitParams({"
    printList(out,params)
    if(!tableIsNoPage(comment)) printList(out,paramsPage)
    out.println "    })\n" +
            "    public Result<List<${className}>> ${method}(@And({"
    printList(out,specs)
    out.println "    }) Specification<${className}> spec,@ApiIgnore " + (count == 6 ? "@SortDefault(sort = {\"${rankField}\"}) " : "") + (tableIsNoPage(comment) ? "Sort sort" : "Pageable pageable") + ") {"
    if (tableIsNoPage(comment)) {
        out.println "        List<${className}> ${javaName}List = ${javaName}Service.get${className}List(spec,sort);"
        out.println "        return Result.ok(${javaName}List);"
    } else {
        out.println "        Page<${className}> ${javaName}Page = ${javaName}Service.get${className}Page(spec,pageable);"
        out.println "        return Result.ok(${javaName}Page.getContent()).setTotal(${javaName}Page.getTotalElements());"
    }
    out.println "    }\n"

    if (tableIsList(comment)) {
        listComment = "不分页"
        out.println "    @ApiOperation(\"${anno}${listComment}\")\n" +
                "    @GetMapping(\"list\")\n" +
                "    @PreAuthorize(\"@el.check(0)\")\n" +
                "    @ApiImplicitParams({"
        printList(out,params)

        out.println "    })\n" +
                "    public Result<List<${className}>> list(@And({"
        printList(out,specs)
        out.println "    }) Specification<${className}> spec,@ApiIgnore " + (count == 6 ? "@SortDefault(sort = {\"${rankField}\"}) " : "") + "Sort sort) {"
        out.println "        List<${className}> ${javaName}List = ${javaName}Service.get${className}List(spec,sort);"
        out.println "        return Result.ok(${javaName}List);"
        out.println "    }\n"
    }

    if (tableIsExport(comment)) {
        listComment = "导出"
        out.println "    @ApiOperation(\"${anno}${listComment}\")\n" +
                "    @GetMapping(\"export\")\n" +
                "    @PreAuthorize(\"@el.check(0)\")\n" +
                "    @ApiImplicitParams({"
        printList(out,params)
        out.println "    })\n" +
                "    public void export(@And({"
        printList(out,specs)
        out.println "    }) Specification<${className}> spec,@ApiIgnore " + (count == 6 ? "@SortDefault(sort = {\"${rankField}\"}) " : "") + "Sort sort, @ApiIgnore HttpServletResponse response) {"
        out.println "        List<${className}> ${javaName}List = ${javaName}Service.get${className}List(spec,sort);"
        out.println "        ExcelUtil.writeHttp(\"excel/${anno}.xls\", \"${anno}\", ${className}.class, ${javaName}List, response);"
        out.println "    }\n"
    }

    out.println "\n" +
            "    @PostMapping\n" +
            "    @ApiOperation(\"增加${anno}\")\n" +
            "    @Log(\"增加${anno}\")\n" +
            "    @PreAuthorize(\"@el.check(0)\")\n" +
            "    public Result<${className}> add(@Valid @RequestBody ${className} ${javaName}) {\n" +
            "        return Result.ok(${javaName}Service.save${className}(${javaName}));\n" +
            "    }\n" +
            "\n" +
            "    @PutMapping(\"{id}\")\n" +
            "    @ApiOperation(\"编辑${anno}\")\n" +
            "    @Log(\"编辑${anno}\")\n" +
            "    @PreAuthorize(\"@el.check(0)\")\n" +
            "    @Transactional\n" +
            "    public Result<${className}> update(@PathVariable(\"id\") ${className} ${javaName}, @RequestBody ObjectNode jsonNode) {\n" +
            "        AppUtils.accept(${javaName}, jsonNode);\n" +
            "        return Result.ok(${javaName}Service.update${className}(${javaName}));\n" +
            "    }\n" +
            "\n" +
            "    @DeleteMapping(\"{id}\")\n" +
            "    @ApiOperation(\"删除${anno}\")\n" +
            "    @Log(\"删除${anno}\")\n" +
            "    @PreAuthorize(\"@el.check(0)\")\n" +
            "    public Result<Integer> delete(@PathVariable(\"id\") Long id) {\n" +
            "        ${javaName}Service.delete${className}(id);\n" +
            "        return Result.ok();\n" +
            "    }\n" +
            "}"
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
        index++
        def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
        def colName = col.getName()
        def isId = primaryKey != null && DasUtil.containsName(colName, primaryKey.getColumnsRef())
        def comm = [
                colName : col.getName(),
                name    : javaName(col.getName(), false),
                type    : typeStr,
                commoent: col.getComment(),
                annos   : " @Column(name = \"" + col.getName() + "\")" + "\n" + "    @JsonProperty(value = \"" + col.getName() + "\",index = " + index + (isId ? ",access = JsonProperty.Access.READ_ONLY" : "") + ")",
                isId    : isId,
        ]
//        if ("id".equals(Case.LOWER.apply(col.getName())))
//            comm.annos += ["@Id"]
        fields += [comm]
    }
}

static boolean contains(String element) {
    return "create_time" == element || "update_time" == element || "create_by" == element || "update_by" == element || "rank" == element || "soft_delete" == element
}

static boolean contains6(String element) {
    return contains(element) || "rank" == element || "soft_delete" == element
}
static boolean containRank(fields){
    boolean rank =false
    fields.each() {
        if (!rank && it.colName == "rank") rank = true
    }
    return rank
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

static void printList(out,params){
    params.each{
        out.println it
    }
}

static String getProjectName(String projectStr){

    def s = "componentStore="
    def e = ")"
    def si = projectStr.indexOf(s)
    def ei = projectStr.indexOf(e,si + s.length())

    return projectStr.substring(si + s.length(),ei)
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