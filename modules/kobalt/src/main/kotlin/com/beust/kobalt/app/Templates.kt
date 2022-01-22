package com.beust.kobalt.app

import com.beust.kobalt.api.ITemplate
import com.beust.kobalt.api.ITemplateContributor
import com.beust.kobalt.app.java.JavaTemplateGenerator
import com.beust.kobalt.app.kotlin.KotlinTemplateGenerator
import com.beust.kobalt.internal.PluginInfo
import com.beust.kobalt.misc.kobaltLog
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ListMultimap

class Templates : ITemplateContributor {
    override val templates = listOf(JavaTemplateGenerator(), KotlinTemplateGenerator())

    fun getTemplates(pluginInfo: PluginInfo): ListMultimap<String, ITemplate> {
        val map = ArrayListMultimap.create<String, ITemplate>()
        pluginInfo.templateContributors.forEach {
            it.templates.forEach {
                map.put(it.pluginName, it)
            }
        }
        return map
    }

    fun displayTemplates(pluginInfo : PluginInfo) {
        val templates = getTemplates(pluginInfo)
        templates.keySet().forEach {
            kobaltLog(1, "  Plug-in: $it")
            templates[it].forEach {
                kobaltLog(1, "    \"" + it.templateName + "\"\t\t" + it.templateDescription)
            }
        }
        kobaltLog(1, "Once you've selected a template, run './kobaltw --init <templateName>")
    }
}
