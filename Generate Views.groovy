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


FILES.chooseDirectoryAndSave("Choose rest directory", "Choose where to store generated files") { dir ->
    SELECTION.filter { it instanceof DasTable && it.getKind() == ObjectKind.TABLE }.each { generate(it, dir) }
}

def generate(table, dir) {
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

    def model = table.getName()
    def javaName = javaName(className,false)
    def anno = isNotEmpty(table.getComment()) ? table.getComment() : "模型"
    out.println "package $packageName"
    out.println ""
    out.println "import com.fasterxml.jackson.databind.node.ObjectNode;\n" +
                "import com.jeiat.itapi.common.Result;\n" +
                "import com.jeiat.itapi.modules.logging.aop.log.Log;\n" +
                "import com.jeiat.itapi.modules.pom.model.${className};\n" +
                "import com.jeiat.itapi.modules.pom.service.${className}Service;\n" +
                "import io.swagger.annotations.Api;\n" +
                "import io.swagger.annotations.ApiImplicitParam;\n" +
                "import io.swagger.annotations.ApiOperation;\n" +
                "import net.kaczmarzyk.spring.data.jpa.domain.Equal;\n" +
                "import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;\n" +
                "import org.springframework.beans.factory.annotation.Autowired;\n" +
                "import org.springframework.data.jpa.domain.Specification;\n" +
                "import org.springframework.security.access.prepost.PreAuthorize;\n" +
                "import org.springframework.web.bind.annotation.*;"
    
    out.println "import java.util.List;"

    out.println ""
    out.println "/**\n" +
            " * Date " + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + " \n" +
            " */"
    out.println ""
    out.println "@Api(tags = \"${anno}接口\")\n" +
                "@RestController\n" +
                "@RequestMapping(\"/api/pom/${model}s\")\n" +
                "public class ${className}Controller {\n" +
                "\n" +
                "    @Autowired\n" +
                "    ${className}Service ${javaName}Service;\n" +
                "\n" +
                "    @ApiOperation(\"${anno}列表\")\n" +
                "    @GetMapping\n" +
                "    @PreAuthorize(\"@el.check(0)\")\n" +
                "    //@ApiImplicitParam(name = \"front_field\", value = \"说明\", dataType = \"integer\", allowableValues = \"0,1\")\n" +
                "    public List<${className}> list(\n" +
                "            //@Spec(path = \"isProduct\", params = \"is_product\", spec = Equal.class)\n" +
                "            Specification<${className}> spec) {\n" +
                "        return ${javaName}Service.get${className}s(spec);\n" +
                "    }\n" +
                "\n" +
                "    @PostMapping\n" +
                "    @ApiOperation(\"增加${anno}\")\n" +
                "    @Log(\"增加${anno}\")\n" +
                "    @PreAuthorize(\"@el.check(0)\")\n" +
                "    public Result<Integer> add(@RequestBody ${className} ${javaName}) {\n" +
                "        ${javaName}Service.save${className}(${javaName});\n" +
                "        return Result.ok();\n" +
                "    }\n" +
                "\n" +
                "    @PutMapping(\"{id}\")\n" +
                "    @ApiOperation(\"编辑${anno}\")\n" +
                "    @Log(\"编辑${anno}\")\n" +
                "    @PreAuthorize(\"@el.check(0)\")\n" +
                "    public Result<Integer> update(@PathVariable(\"id\") ${className} ${javaName}, @RequestBody ObjectNode jsonNode) {\n" +
                "\n" +
                "        ${javaName}.accept(jsonNode);\n" +
                "        ${javaName}Service.update${className}(${javaName});\n" +
                "\n" +
                "        return Result.ok();\n" +
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
