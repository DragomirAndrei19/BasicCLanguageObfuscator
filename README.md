# BasicCLanguageObfuscator
A basic C language obfuscator written in Java taking advantage of regular expressions/RegExr

![alt text](https://i.imgur.com/6UcdrH6.jpg)
![alt text](https://i.imgur.com/Z2uM01m.jpg)


### How to run -> Command line:
CObfuscator.jar unbfuscatedSrc.c 
# Alternatively, the runnable jar file can be generated using the source code file

# Features:

1) Hardcoded string obfuscation
2) Numeric constants obfuscations
3) Variable names replacer 
4) Deletion of unnecessary spaces and line breaks
5) Generates an output file that appends a truly random generated (TRNG) number to the original filename. 
   Uses API calls to random.org API (randomness via atmospheric noise)
   
### How the result looks like
# Check EXAMPLES folder for more examples
Original file: example.c
Output file: example213456.c


       
