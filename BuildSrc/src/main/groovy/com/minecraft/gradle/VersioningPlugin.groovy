package com.minecraft.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

class VersioningPlugin implements Plugin<Project> {
    
    void apply(Project project) {
        // Create version extension
        project.extensions.create('versioning', VersioningExtension)
        
        // Add version management tasks
        addVersionTasks(project)
        
        // Initialize versions
        initializeVersions(project)
    }
    
    void addVersionTasks(Project project) {
        // Task to bump major version
        project.task('bumpMajor') {
            group = 'versioning'
            description = 'Bump major version (x.0.0)'
            
            doLast {
                def versions = loadVersions(project)
                String moduleName = project.name
                if (moduleName == project.rootProject.name) {
                    moduleName = 'root'
                }
                
                def moduleVersion = versions[moduleName] ?: [1, 0, 0]
                moduleVersion[0] += 1
                moduleVersion[1] = 0
                moduleVersion[2] = 0
                
                versions[moduleName] = moduleVersion
                saveVersions(project, versions)
                
                println "Bumped major version of $moduleName to ${moduleVersion[0]}.${moduleVersion[1]}.${moduleVersion[2]}"
            }
        }
        
        // Task to bump minor version
        project.task('bumpMinor') {
            group = 'versioning'
            description = 'Bump minor version (0.x.0)'
            
            doLast {
                def versions = loadVersions(project)
                String moduleName = project.name
                if (moduleName == project.rootProject.name) {
                    moduleName = 'root'
                }
                
                def moduleVersion = versions[moduleName] ?: [1, 0, 0]
                moduleVersion[1] += 1
                moduleVersion[2] = 0
                
                versions[moduleName] = moduleVersion
                saveVersions(project, versions)
                
                println "Bumped minor version of $moduleName to ${moduleVersion[0]}.${moduleVersion[1]}.${moduleVersion[2]}"
            }
        }
        
        // Task to bump patch version
        project.task('bumpPatch') {
            group = 'versioning'
            description = 'Bump patch version (0.0.x)'
            
            doLast {
                def versions = loadVersions(project)
                String moduleName = project.name
                if (moduleName == project.rootProject.name) {
                    moduleName = 'root'
                }
                
                def moduleVersion = versions[moduleName] ?: [1, 0, 0]
                moduleVersion[2] += 1
                
                versions[moduleName] = moduleVersion
                saveVersions(project, versions)
                
                println "Bumped patch version of $moduleName to ${moduleVersion[0]}.${moduleVersion[1]}.${moduleVersion[2]}"
            }
        }
        
        // Task to display current version
        project.task('showVersion') {
            group = 'versioning'
            description = 'Show current version'
            
            doLast {
                def versions = loadVersions(project)
                String moduleName = project.name
                if (moduleName == project.rootProject.name) {
                    moduleName = 'root'
                }
                
                def moduleVersion = versions[moduleName] ?: [1, 0, 0]
                println "$moduleName version: ${moduleVersion[0]}.${moduleVersion[1]}.${moduleVersion[2]}"
            }
        }
    }
    
    void initializeVersions(Project project) {
        project.afterEvaluate {
            def versions = loadVersions(project)
            String moduleName = project.name
            if (moduleName == project.rootProject.name) {
                moduleName = 'root'
            }
            
            def moduleVersion = versions[moduleName] ?: [1, 0, 0]
            def versionString = "${moduleVersion[0]}.${moduleVersion[1]}.${moduleVersion[2]}"
            
            // Set project version
            project.version = versionString
            
            // Set project ext properties for reference in build scripts
            project.ext.majorVersion = moduleVersion[0]
            project.ext.minorVersion = moduleVersion[1]
            project.ext.patchVersion = moduleVersion[2]
            
            println "Set $moduleName version to $versionString"
        }
    }
    
    Map<String, List<Integer>> loadVersions(Project project) {
        def versionsFile = getVersionsFile(project)
        if (!versionsFile.exists()) {
            versionsFile.parentFile.mkdirs()
            versionsFile.text = '{}'
            return [:]
        }
        
        def slurper = new JsonSlurper()
        return slurper.parse(versionsFile)
    }
    
    void saveVersions(Project project, Map<String, List<Integer>> versions) {
        def versionsFile = getVersionsFile(project)
        versionsFile.parentFile.mkdirs()
        versionsFile.text = JsonOutput.prettyPrint(JsonOutput.toJson(versions))
    }
    
    File getVersionsFile(Project project) {
        return new File(project.rootProject.projectDir, ".gradle/versions.json")
    }
}

class VersioningExtension {
    
}