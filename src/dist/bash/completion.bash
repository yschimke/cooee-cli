#@IgnoreInspection BashAddShebang
_cooee_complete()
{
  local cur prev words cword
  COMPREPLY=()
	job="${COMP_WORDS[0]}"
	cur="${COMP_WORDS[COMP_CWORD]}"
	prev="${COMP_WORDS[COMP_CWORD-1]}"

	_get_comp_words_by_ref -n : cur

  case ${prev} in
        # options with an argument we don't currently help with, everything else is assumed to be handled
        # below in case statement or has no arguments so drops through to the url handling near the end
#        -i | --input)
#            return
#            ;;
        --option-complete)
            _cooee_complete=$(cooee --option-complete complete)
            COMPREPLY=( $( compgen -W "${_cooee_complete}" -- "$cur" ) )
            return
            ;;
  esac

  if [[ ${cur} == -* ]]; then
      # TODO parse help automatically
      #_cooee_options=${_cooee_options:=$(_parse_help cooee --help)}
      _cooee_options="-h --help -V --version -l --local --command-complete --option-complete --debug"
      COMPREPLY=( $( compgen -W "$_cooee_options" -- "$cur" ) )
      return;
  fi

  # TODO cache completions
  _cooee_commands=$(cooee --command-complete "${COMP_LINE/#cooee }" "$cur" "$COMP_POINT")
  COMPREPLY=( $( compgen -W "$_cooee_commands" -- "$cur" ) )

  __ltrim_colon_completions "$cur"
}

complete -F _cooee_complete cooee
