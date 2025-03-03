package top.eternal.neyran.movementUI.managers

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import top.eternal.neyran.movementUI.MovementsMain
import java.io.File
import java.io.InputStreamReader


class ConfigManager(private val plugin: MovementsMain) {
    lateinit var configFile: File
    lateinit var customConfig: FileConfiguration
    lateinit var settingsFile: File
    lateinit var settingsConfig: FileConfiguration
    lateinit var langFile: File
    lateinit var langConfig: FileConfiguration
    lateinit var bindFile: File
    lateinit var bindConfig: FileConfiguration
    init {
        loadConfigs()
    }
    fun loadConfigs() {
        settingsFile = File(plugin.dataFolder, "config.yml")
        bindFile = File(plugin.dataFolder, "bind.yml")
        langFile = File(plugin.dataFolder, "lang.yml")
        val menusDir = File(plugin.dataFolder, "menus")

        if (!settingsFile.exists()) {
            plugin.saveResource("config.yml", false)
        }
        if (!bindFile.exists()) {
            plugin.saveResource("bind.yml", false)
        }
        if (!langFile.exists()) {
            plugin.saveResource("lang.yml", false)
        }
        if (!menusDir.exists()) {
            menusDir.mkdirs()
            plugin.saveResource("menus/default_menus.yml", false)
        }
        configFile = menusDir
        customConfig = loadVirtualConfig(menusDir)

        bindConfig = YamlConfiguration.loadConfiguration(bindFile)
        settingsConfig = YamlConfiguration.loadConfiguration(settingsFile)
        langConfig = YamlConfiguration.loadConfiguration(langFile)
        validateConfig("config.yml", settingsConfig)
        validateConfig("lang.yml", langConfig)
    }
    fun loadLanguage() {
        val langFile = File(plugin.dataFolder, "lang.yml")

        if (settingsConfig.getBoolean("custom_lang", false)) {
            langConfig = YamlConfiguration.loadConfiguration(langFile)
            plugin.logger.info("Custom language mode enabled. Using lang.yml without overwriting.")
            return
        }

        val langCode = settingsConfig.getString("lang", "EN_US")
        val resourcePath = "langs/$langCode.yml"

        val inputStream = plugin.getResource(resourcePath)
        if (inputStream != null) {
            val reader = InputStreamReader(inputStream, Charsets.UTF_8)
            langConfig = YamlConfiguration.loadConfiguration(reader)
            plugin.logger.info("Loaded language preset: $langCode from JAR")
        } else {
            plugin.logger.warning("Language file $resourcePath not found in JAR!")
        }
    }


     fun loadVirtualConfig(dir: File): YamlConfiguration {
        val config = YamlConfiguration()
        val configs = dir.listFiles { _, name -> name.endsWith(".yml") }
            ?.map { YamlConfiguration.loadConfiguration(it) } ?: emptyList()

        configs.forEach { yaml ->
            yaml.getKeys(true).forEach { key ->
                if (!config.isSet(key)) {
                    config.set(key, yaml.get(key))
                }
            }
        }
        return config
    }
    private fun validateConfig(resourceName: String, fileConfig: FileConfiguration) {
        val defaultConfig = YamlConfiguration.loadConfiguration(plugin.getResource(resourceName)?.reader() ?: return)

        defaultConfig.getKeys(true).forEach { key ->
            if (!fileConfig.contains(key)) {
                fileConfig.set(key, defaultConfig.get(key))
                plugin.logger.info("[MovementUI] Added missing key '$key' to $resourceName")
            }
        }

        fileConfig.save(File(plugin.dataFolder, resourceName))
    }
    fun reloadConfigs() {
        plugin.configManager.customConfig = plugin.configManager.loadVirtualConfig(File(plugin.dataFolder, "menus"))
        plugin.configManager.bindConfig = YamlConfiguration.loadConfiguration(plugin.configManager.bindFile)
        plugin.configManager.settingsConfig = YamlConfiguration.loadConfiguration(plugin.configManager.settingsFile)
        plugin.configManager.langConfig = YamlConfiguration.loadConfiguration(plugin.configManager.langFile)

        plugin.configManager.loadLanguage()
    }
}