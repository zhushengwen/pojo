import java.io.*
import com.intellij.database.extensions.ExtensionScriptsUtil.*

String dir = "C:\\Users\\Administrator\\AppData\\Roaming\\JetBrains\\IntelliJIdea2022.2\\extensions\\com.intellij.database\\schema\\"

evaluate(new File(dir + "Generate POJOs.groovy").getText('UTF-8'))
evaluate(new File(dir + "Generate Repos.groovy").getText('UTF-8'))
evaluate(new File(dir + "Generate Srvcs.groovy").getText('UTF-8'))
evaluate(new File(dir + "Generate Views.groovy").getText('UTF-8'))
