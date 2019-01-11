function __fish_complete_cooee_command
	cooee --fish-complete (commandline -t)
end

complete -c cooee -x -d 'Cooee CLI' -a '(__fish_complete_cooee_command)'
complete -c cooee -l debug -d 'Show debug output'
complete -c cooee -s h -l help -d 'Help options'
complete -c cooee -l login -d 'Login and link account'
complete -c cooee -l logout -d 'Unlink account'
complete -c cooee -s V -l version -d 'Output version and exit'

complete -c cooee -l token -x -d 'Provide specific authorization token'
#complete -c cooee -l tokenSet -x -d 'Set a named authorization token'

complete -c cooee -l authorize -x -d 'Authorize a provider' -a "atlassian github google trello strava"
#complete -c cooee -l option-complete -x -a "option-complete command-complete authorize" -d 'Complete possible options'

complete -c cooee -l command-complete -d 'Complete possible command'
