// ./BuildSrc/src/main/groovy/com/minecraft/gradle/GitHubPlugin.groovy
package com.minecraft.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

class GitHubPlugin implements Plugin<Project> {
    
    void apply(Project project) {
        // Create GitHub extension
        project.extensions.create('github', GitHubExtension)
        
        // Only add GitHub tasks to the root project
        if (project == project.rootProject) {
            addGitHubTasks(project)
        }
    }
    
    void addGitHubTasks(Project project) {
        // Task to create gitignore file
        project.task('createGitignore') {
            group = 'github'
            description = 'Create a .gitignore file if it does not exist'
            
            doLast {
                def gitignoreFile = new File(project.projectDir, '.gitignore')
                if (!gitignoreFile.exists()) {
                    gitignoreFile.text = """
# Gradle files
.gradle
.gradle/
**/.gradle/
build/
!gradle/wrapper/gradle-wrapper.jar
!**/src/main/**/build/
!**/src/test/**/build/

# IntelliJ IDEA
.idea/
*.iws
*.iml
*.ipr
out/
!**/src/main/**/out/
!**/src/test/**/out/

# Eclipse
.apt_generated
.classpath
.factorypath
.project
.settings
.springBeans
.sts4-cache
bin/
!**/src/main/**/bin/
!**/src/test/**/bin/

# NetBeans
/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.nb-gradle/

# VS Code
.vscode/

# Mac OS
.DS_Store

# Windows
Thumbs.db
ehthumbs.db
Desktop.ini

# Logs
*.log
logs/

# Virtual Machine crash logs
hs_err_pid*

# Credentials and tokens
# NEVER commit real credentials
**/local.properties
**/*credentials*
**/*token*

# Build outputs
**/out/
**/target/
"""
                    println "Created .gitignore file in ${project.projectDir}"
                } else {
                    println ".gitignore file already exists in ${project.projectDir}"
                }
            }
        }
        
        // Task to initialize Git repository
        project.task('gitInit') {
            group = 'github'
            description = 'Initialize Git repository'
            dependsOn project.tasks.createGitignore
            
            doLast {
                def gitFolder = new File(project.projectDir, '.git')
                if (!gitFolder.exists()) {
                    executeCommand(project, 'git init')
                    println "Initialized Git repository in ${project.projectDir}"
                    
                    // Configure Git user if not already configured
                    try {
                        def userName = executeCommand(project, 'git config user.name')
                        if (userName.isEmpty()) {
                            def configuredName = project.findProperty('github.username') ?: System.getenv('GITHUB_USERNAME') ?: "Gradle Build"
                            executeCommand(project, "git config user.name \"${configuredName}\"")
                            println "Set Git user.name to $configuredName"
                        }
                        
                        def userEmail = executeCommand(project, 'git config user.email')
                        if (userEmail.isEmpty()) {
                            def configuredEmail = project.findProperty('github.email') ?: System.getenv('GITHUB_EMAIL') ?: "build@example.com"
                            executeCommand(project, "git config user.email \"${configuredEmail}\"")
                            println "Set Git user.email to $configuredEmail"
                        }
                    } catch (Exception e) {
                        println "Warning: Could not configure Git user: ${e.message}"
                    }
                } else {
                    println "Git repository already exists in ${project.projectDir}"
                }
            }
        }
        
        // Task to create GitHub repository
        project.task('createGitHubRepo') {
            group = 'github'
            description = 'Create GitHub repository if it does not exist'
            dependsOn project.tasks.gitInit  // Make sure Git is initialized first
            
            doLast {
                def githubToken = project.findProperty('github.token') ?: System.getenv('GITHUB_TOKEN')
                if (!githubToken) {
                    throw new IllegalStateException("GitHub token not found. Set it in gradle.properties as github.token or as GITHUB_TOKEN environment variable.")
                }
                
                def githubUsername = project.findProperty('github.username') ?: System.getenv('GITHUB_USERNAME')
                if (!githubUsername) {
                    throw new IllegalStateException("GitHub username not found. Set it in gradle.properties as github.username or as GITHUB_USERNAME environment variable.")
                }
                
                // For monorepo approach, use the rootRepoName or the root project name
                String repoName = project.github.rootRepoName ?: project.name
                
                // Check if repository exists
                def repoExists = checkRepoExists(githubUsername, repoName, githubToken)
                
                if (!repoExists) {
                    // Create repository using Java's HttpURLConnection to avoid shell escaping issues
                    def isPrivate = project.github.privateRepo ? true : false
                    def description = project.description ?: "Monorepo for Minecraft plugins"
                    
                    def jsonPayload = JsonOutput.toJson([
                        name: repoName,
                        description: description,
                        private: isPrivate
                    ])
                    
                    def url = new URL("https://api.github.com/user/repos")
                    def connection = url.openConnection()
                    connection.setRequestMethod("POST")
                    connection.setRequestProperty("Authorization", "token ${githubToken}")
                    connection.setRequestProperty("Accept", "application/vnd.github+json")
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setDoOutput(true)
                    
                    try {
                        def writer = new OutputStreamWriter(connection.getOutputStream())
                        writer.write(jsonPayload)
                        writer.flush()
                        writer.close()
                        
                        def responseCode = connection.getResponseCode()
                        
                        if (responseCode >= 200 && responseCode < 300) {
                            println "Created GitHub repository: ${githubUsername}/${repoName}"
                        } else {
                            def errorStream = connection.getErrorStream()
                            def errorResponse = ""
                            if (errorStream != null) {
                                errorResponse = new BufferedReader(new InputStreamReader(errorStream)).getText()
                            }
                            println "Error creating GitHub repository: ${errorResponse}"
                        }
                    } catch (Exception e) {
                        println "Error creating GitHub repository: ${e.message}"
                    }
                } else {
                    println "GitHub repository ${githubUsername}/${repoName} already exists"
                }
                
                // Set remote origin if not already set
                boolean remoteExists = false
                try {
                    def remoteOutput = executeCommand(project, 'git remote')
                    remoteExists = remoteOutput.contains("origin")
                } catch (Exception e) {
                    println "Warning: Could not check Git remote: ${e.message}"
                }
                
                if (!remoteExists) {
                    executeCommand(project, "git remote add origin https://github.com/${githubUsername}/${repoName}.git")
                    println "Added remote origin: https://github.com/${githubUsername}/${repoName}.git"
                }
            }
        }
        
        // Task to stage changes without committing
        project.task('gitStage') {
            group = 'github'
            description = 'Add files to Git staging area'
            dependsOn project.tasks.gitInit
            
            doLast {
                println "Staging all files to Git..."
                // Add everything in the repository
                executeCommand(project, "git add .")
                println "Files staged. Use 'git status' to see what will be committed."
            }
        }
        
        // Task to commit staged changes
        project.task('gitCommit') {
            group = 'github'
            description = 'Commit staged changes to Git'
            dependsOn project.tasks.gitStage
            
            doLast {
                def commitMessage = project.findProperty('commit.message') ?: "Update to version ${project.version}"
                println "Committing changes with message: ${commitMessage}"
                
                executeCommand(project, "git commit -m \"${commitMessage}\"")
                
                println "Changes committed. Use 'git log' to see commit history."
            }
        }
        
        // Task to set up remote repository
        project.task('gitSetupRemote') {
            group = 'github'
            description = 'Set up remote GitHub repository'
            
            doLast {
                def githubUsername = project.findProperty('github.username') ?: System.getenv('GITHUB_USERNAME')
                String repoName = project.github.rootRepoName ?: project.name
                
                println "Checking for remote 'origin'..."
                def remoteExists = false
                try {
                    def remoteOutput = executeCommand(project, 'git remote')
                    remoteExists = remoteOutput.contains('origin')
                } catch (Exception e) {
                    println "Error checking remote: ${e.message}"
                }
                
                if (!remoteExists) {
                    println "Adding remote origin: https://github.com/${githubUsername}/${repoName}.git"
                    executeCommand(project, "git remote add origin https://github.com/${githubUsername}/${repoName}.git")
                } else {
                    println "Remote 'origin' already exists. Setting URL to: https://github.com/${githubUsername}/${repoName}.git"
                    executeCommand(project, "git remote set-url origin https://github.com/${githubUsername}/${repoName}.git")
                }
                
                println "Remote repository set up. Use 'git remote -v' to verify."
            }
        }
        
        // Enhanced pushToGitHub task with timeout handling and better debugging
        project.task('pushToGitHub') {
            group = 'github'
            description = 'Commit and push to GitHub with improved error handling'
            dependsOn project.tasks.gitInit, project.tasks.createGitHubRepo
            
            doLast {
                // Enable debug mode for more detailed output
                def debug = project.hasProperty('debug') ?: false
                
                // Get branch information
                def branchName = project.findProperty('git.branch') ?: 'main'
                println "Using branch: ${branchName}"
                
                // Check Git status first
                def hasGitRepo = new File(project.projectDir, '.git').exists()
                if (!hasGitRepo) {
                    println "No Git repository found. Running git init..."
                    executeCommand(project, 'git init', debug)
                }
                
                // Check if we have any commits yet
                def hasCommits = executeCommand(project, 'git log -1 --oneline', debug, true) != ""
                println "Repository has commits: ${hasCommits}"
                
                // Check and setup branch
                if (hasCommits) {
                    // Get current branch
                    def currentBranch = executeCommand(project, 'git branch --show-current', debug)
                    println "Current branch: ${currentBranch}"
                    
                    // If different branch, rename or checkout
                    if (currentBranch != branchName && !currentBranch.isEmpty()) {
                        // Check if target branch exists
                        def branchExists = executeCommand(project, 'git branch', debug).contains(branchName)
                        
                        if (branchExists) {
                            // Checkout existing branch
                            println "Checking out existing branch: ${branchName}"
                            executeCommand(project, "git checkout ${branchName}", debug)
                        } else if (currentBranch == "master" && branchName == "main") {
                            // Rename master to main (common case)
                            println "Renaming branch from master to main"
                            executeCommand(project, 'git branch -m master main', debug)
                        } else {
                            // Create and checkout new branch
                            println "Creating and checking out new branch: ${branchName}"
                            executeCommand(project, "git checkout -b ${branchName}", debug)
                        }
                    }
                }
                
                // Add all files not in .gitignore
                println "Adding all project files to Git..."
                executeCommand(project, "git add .", debug)
                
                // Check if we have changes to commit
                def statusOutput = executeCommand(project, 'git status --porcelain', debug)
                def hasChanges = !statusOutput.trim().isEmpty()
                
                if (hasChanges) {
                    println "Changes detected, committing..."
                    println "Status: ${statusOutput}"
                    
                    // Commit changes
                    def commitMessage = project.findProperty('commit.message') ?: "Update to version ${project.version}"
                    executeCommand(project, "git commit -m \"${commitMessage}\"", debug)
                    
                    // Check if we need to set up the remote
                    def remoteOutput = executeCommand(project, 'git remote', debug)
                    def remoteExists = remoteOutput.contains("origin")
                    
                    if (!remoteExists) {
                        // Add remote
                        def githubUsername = project.findProperty('github.username') ?: System.getenv('GITHUB_USERNAME')
                        String repoName = project.github.rootRepoName ?: project.name
                        
                        println "Adding remote origin: https://github.com/${githubUsername}/${repoName}.git"
                        executeCommand(project, "git remote add origin https://github.com/${githubUsername}/${repoName}.git", debug)
                    }
                    
                    // Push to GitHub using token from environment
                    def githubToken = System.getenv('GITHUB_TOKEN')
                    def githubUsername = project.findProperty('github.username') ?: System.getenv('GITHUB_USERNAME')
                    
                    if (githubToken && githubUsername) {
                        String repoName = project.github.rootRepoName ?: project.name
                        
                        // Set remote URL with credentials
                        def remoteUrl = "https://${githubUsername}:${githubToken}@github.com/${githubUsername}/${repoName}.git"
                        executeCommand(project, "git remote set-url origin ${remoteUrl}", debug)
                        
                        // Push to GitHub with timeout
                        println "Pushing to GitHub... (this may take a moment)"
                        
                        try {
                            // Use ProcessBuilder for better control and timeout
                            def processBuilder = new ProcessBuilder("git", "push", "-u", "origin", branchName)
                            processBuilder.directory(project.projectDir)
                            processBuilder.redirectErrorStream(true)
                            
                            def process = processBuilder.start()
                            def reader = new BufferedReader(new InputStreamReader(process.getInputStream()))
                            
                            // Set timeout for push operation
                            def timeout = 60 // seconds
                            def pushCompleted = process.waitFor(timeout, java.util.concurrent.TimeUnit.SECONDS)
                            
                            // Read all output
                            def line
                            while ((line = reader.readLine()) != null) {
                                println line
                            }
                            
                            if (!pushCompleted) {
                                // Kill the process if it timed out
                                process.destroy()
                                println "Push operation timed out after ${timeout} seconds."
                                println "This might be due to credential issues or network problems."
                                println "Try pushing manually with: git push -u origin ${branchName}"
                            } else if (process.exitValue() != 0) {
                                println "Push failed with exit code: ${process.exitValue()}"
                            } else {
                                println "Push completed successfully!"
                            }
                        } catch (Exception e) {
                            println "Error during Git push: ${e.message}"
                            e.printStackTrace()
                        }
                        
                        // Reset remote URL to not store credentials
                        executeCommand(project, "git remote set-url origin https://github.com/${githubUsername}/${repoName}.git", debug)
                    } else {
                        println "GITHUB_TOKEN environment variable not set. Please set it before pushing."
                        println "For example: set GITHUB_TOKEN=your_token_here"
                    }
                } else {
                    println "No changes to commit"
                }
            }
        }
        
        // Task to tag release
        project.task('tagRelease') {
            group = 'github'
            description = 'Create and push git tag for current version'
            
            doLast {
                def version = project.version
                def tagName = "v${version}"
                
                // Create tag
                executeCommand(project, "git tag -a ${tagName} -m \"Release ${version}\"")
                
                // Push tag
                executeCommand(project, "git push origin ${tagName}")
                
                println "Created and pushed tag: ${tagName}"
            }
        }
        
        // Task to release project to GitHub
        project.task('releaseToGitHub') {
            group = 'github'
            description = 'Release project to GitHub (bump patch version, commit, tag, and push)'
            dependsOn project.tasks.bumpPatch, project.tasks.build
            
            doLast {
                // First run the build
                project.tasks.pushToGitHub.execute()
                project.tasks.tagRelease.execute()
                
                println "Released version ${project.version} to GitHub"
            }
        }
        
        // Task to clean tracked files that are now in .gitignore
        project.task('gitCleanIgnored') {
            group = 'github'
            description = 'Remove previously committed files that are now in .gitignore from the repository'
            
            doLast {
                println "Checking for tracked files that should be ignored..."
                
                // Get list of tracked files that are ignored according to current rules
                def ignoredFiles = executeCommand(project, "git ls-files -ci --exclude-standard")
                
                if (ignoredFiles.trim().isEmpty()) {
                    println "No tracked files found that should be ignored."
                } else {
                    // Display the files that will be removed from tracking
                    println "The following files will be removed from Git tracking (but kept on disk):"
                    ignoredFiles.split('\n').each { file ->
                        if (!file.trim().isEmpty()) {
                            println "  - ${file}"
                        }
                    }
                    
                    // Check if we should skip confirmation (for CI/CD or non-interactive environments)
                    boolean skipConfirmation = project.hasProperty('skipConfirmation') && 
                        project.property('skipConfirmation').toString().toBoolean()
                    
                    boolean proceed = skipConfirmation
                    
                    if (!skipConfirmation) {
                        // Try to get confirmation from the user
                        try {
                            def scanner = new java.util.Scanner(System.in)
                            print "Do you want to proceed? (y/n): "
                            def answer = scanner.nextLine().trim().toLowerCase()
                            proceed = (answer == 'y' || answer == 'yes')
                        } catch (Exception e) {
                            println "Could not get user input. Use -PskipConfirmation=true to skip confirmation."
                            println "Operation cancelled."
                            return
                        }
                    } else {
                        println "Skipping confirmation as requested."
                    }
                    
                    if (proceed) {
                        // Get the list of files to remove from tracking
                        ignoredFiles.split('\n').each { file ->
                            if (!file.trim().isEmpty()) {
                                println "Removing ${file} from Git tracking..."
                                executeCommand(project, "git rm --cached \"${file}\"")
                            }
                        }
                        
                        // Commit the changes
                        println "Committing changes..."
                        executeCommand(project, "git commit -m \"Remove ignored files from Git tracking\"")
                        
                        println "Successfully removed ignored files from Git tracking. They remain on disk but are no longer tracked."
                        println "Use 'git push' to update the remote repository."
                    } else {
                        println "Operation cancelled."
                    }
                }
            }
        }
    }
    
    boolean checkRepoExists(String username, String repo, String token) {
        try {
            def url = new URL("https://api.github.com/repos/${username}/${repo}")
            def connection = url.openConnection()
            connection.setRequestMethod("GET")
            connection.setRequestProperty("Authorization", "token ${token}")
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            
            return connection.getResponseCode() == 200
        } catch (Exception e) {
            println "Warning: Could not check if repository exists: ${e.message}"
            return false
        }
    }
    
    // Helper method to execute commands with timeout
    String executeCommand(Project project, String command, boolean debug = false, boolean returnOutput = true) {
        def output = new StringBuilder()
        def error = new StringBuilder()
        
        if (debug) {
            println "Executing: ${command}"
        }
        
        try {
            // Split command into parts for ProcessBuilder
            def parts = command.split(' ')
            def processBuilder = new ProcessBuilder(parts)
            processBuilder.directory(project.projectDir)
            
            def process = processBuilder.start()
            
            // Set up output and error readers
            def outReader = new BufferedReader(new InputStreamReader(process.getInputStream()))
            def errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))
            
            // Read output and error streams
            def outThread = new Thread({
                def line
                while ((line = outReader.readLine()) != null) {
                    output.append(line).append('\n')
                    if (debug) println "OUT: ${line}"
                }
            })
            
            def errThread = new Thread({
                def line
                while ((line = errReader.readLine()) != null) {
                    error.append(line).append('\n')
                    if (debug) println "ERR: ${line}"
                }
            })
            
            outThread.start()
            errThread.start()
            
            // Wait for process to complete with timeout
            def timeout = 30 // seconds
            def completed = process.waitFor(timeout, java.util.concurrent.TimeUnit.SECONDS)
            
            outThread.join(5000)
            errThread.join(5000)
            
            if (!completed) {
                process.destroy()
                println "Command timed out after ${timeout} seconds: ${command}"
            } else if (process.exitValue() != 0 && !returnOutput) {
                println "Command failed: ${command}"
                println "Error output: ${error.toString()}"
            }
        } catch (Exception e) {
            println "Exception executing command: ${e.message}"
            if (debug) e.printStackTrace()
        }
        
        return output.toString().trim()
    }
}

class GitHubExtension {
    boolean privateRepo = false
    String rootRepoName = null
}