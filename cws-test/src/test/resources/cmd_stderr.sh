#!/bin/bash

echo 'OUT1';
echo 'OUT2';
echo 'ERR3' 1>&2
echo 'OUT4'
echo 'ERR5' 1>&2
echo 'OUT6'
echo 'OUT7';
echo 'ERR8' 1>&2
echo 'OUT9'
echo 'ERR10' 1>&2
echo 'OUT11'

exit 0;
