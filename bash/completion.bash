#!/usr/bin/env bash

if [ -n "$BASH_VERSION" ]; then
  # Enable programmable completion facilities when using bash (see [3])
  shopt -s progcomp
fi

function CompWordsContainsArray() {
  declare -a localArray
  localArray=("$@")
  local findme
  for findme in "${localArray[@]}"; do
    if ElementNotInCompWords "$findme"; then return 1; fi
  done
  return 0
}

function ElementNotInCompWords() {
  local findme="$1"
  local element
  for element in "${COMP_WORDS[@]}"; do
    if [[ "$findme" = "$element" ]]; then return 1; fi
  done
  return 0
}

function currentPositionalIndex() {
  local commandName="$1"
  local optionsWithArgs="$2"
  local booleanOptions="$3"
  local previousWord
  local result=0

  for i in $(seq $((COMP_CWORD - 1)) -1 0); do
    previousWord=${COMP_WORDS[i]}
    if [ "${previousWord}" = "$commandName" ]; then
      break
    fi
    if [[ "${optionsWithArgs}" =~ ${previousWord} ]]; then
      ((result-=2)) # Arg option and its value not counted as positional param
    elif [[ "${booleanOptions}" =~ ${previousWord} ]]; then
      ((result-=1)) # Flag option itself not counted as positional param
    fi
    ((result++))
  done
  echo "$result"
}

# Bash completion entry point function.
# _complete_cooee finds which commands and subcommands have been specified
# on the command line and delegates to the appropriate function
# to generate possible options and subcommands for the last specified subcommand.
function _complete_cooee() {
  # No subcommands were specified; generate completions for the top-level command.
  _picocli_cooee; return $?;
}

# Generates completions for the options and subcommands of the `cooee` command.
function _picocli_cooee() {
  # Get completion data
  local curr_word=${COMP_WORDS[COMP_CWORD]}
  local prev_word=${COMP_WORDS[COMP_CWORD-1]}

  local flag_opts="--command-complete --debug --open --local -h --help -V --version"
  local arg_opts="--option-complete"

  case ${prev_word} in
    --option-complete)
      return
      ;;
  esac

  if [[ "${curr_word}" == -* ]]; then
    COMPREPLY=( $(compgen -W "${flag_opts} ${arg_opts}" -- "${curr_word}") )
  else
    local positionals=$( cooee --command-complete "$COMP_WORDS" | cut -d ':' -f 1 )
    COMPREPLY=( $(compgen -W "${positionals}" -- "${curr_word}") )
  fi
}

complete -F _complete_cooee cooee
