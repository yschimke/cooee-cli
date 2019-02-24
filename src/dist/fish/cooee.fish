function __fish_complete_cooee_command
	cooee --fish-complete (commandline -ct)
end

function __fish_complete_add_command
	cooee --option-complete (commandline -ct)
end

function __fish_complete_remove_command
	cooee --option-complete (commandline -ct)
end

complete -c cooee -x -d 'Cooee CLI' -a '(__fish_complete_cooee_command)'
complete -c cooee -l debug -d 'Show debug output'
complete -c cooee -s h -l help -d 'Help options'
complete -c cooee -l login -d 'Login and link account'
complete -c cooee -l logout -d 'Unlink account'
complete -c cooee -s V -l version -d 'Output version and exit'
complete -c cooee -l repl -d 'REPL'

complete -c cooee -l list -d 'List apps'
complete -c cooee -l add -x -d 'Add app' -a '(cooee --option-complete add)'
complete -c cooee -l remove -x -d 'Remove app' -a '(cooee --option-complete remove)'

complete -c cooee -l token -x -d 'Provide specific authorization token'
#complete -c cooee -l tokenSet -x -d 'Set a named authorization token'

complete -c cooee -l authorize -x -d 'Authorize a provider' -a "atlassian github google trello strava"
complete -c cooee -l option-complete -x -a "option-complete command-complete authorize" -d 'Complete possible options'

complete -c cooee -l command-complete -d 'Complete possible command'
