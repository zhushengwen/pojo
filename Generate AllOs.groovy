import java.io.*

String dir = "C:\\Users\\Administrator\\AppData\\Roaming\\JetBrains\\IntelliJIdea2022.2\\extensions\\com.intellij.database\\schema\\"

evaluate(new File(dir + "Generate POJOs.groovy"))
evaluate(new File(dir + "Generate Repos.groovy"))
evaluate(new File(dir + "Generate Srvcs.groovy"))
evaluate(new File(dir + "Generate Views.groovy"))
