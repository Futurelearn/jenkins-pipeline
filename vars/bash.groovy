#!/usr/bin/env groovy

/**
* Run a command in bash rather than sh
*
*
*/

def call(String command) {
  sh """#!/bin/bash
        ${command}
  """
}
