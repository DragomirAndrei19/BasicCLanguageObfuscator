import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CObfuscator {

	public static LinkedHashMap<String, String> defineKeywordsHashMap = new LinkedHashMap<String, String>();
	public static LinkedHashMap<String, String> defineNumericHashMap = new LinkedHashMap<String, String>();
	public static Set<String> variablesAndFunctionsSet = new HashSet<String>();
	public static Set<String> numericConstantsSet = new HashSet<String>();
	public static String[] variablesAndFunctionsList = null;
	public static String[] numericConstantsList = null;
	public static boolean commentClosed = true;

	public static void main(String[] args) throws IOException {
		if (args.length > 0) {

			// 1. We open the file (1st pass) to identify variables, function names, numeric cosntants
			// We do not need to modify anything inside the code yet
			File originalCodeFile = new File(args[0]);
			FileReader fileReader = new FileReader(originalCodeFile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			StringBuffer stringBuffer = new StringBuffer();
			String lineOfCode;

			// Identifying variables&function names and numeric constants and putting them in a HashSet
			while ((lineOfCode = bufferedReader.readLine()) != null) {
				identifyVarAndFuncNames(lineOfCode);
				identifyNumericCostants(lineOfCode);
			}
			fileReader.close();
			bufferedReader.close();
			

			// 2. Transforming sets into arrays so we can order them (HashSets don't maintain order)
			// We need them ordered from largest to smallest because we will replace the longest variables & constants first
			variablesAndFunctionsList = variablesAndFunctionsSet.toArray(new String[variablesAndFunctionsSet.size()]);
			Arrays.sort(variablesAndFunctionsList, (a, b) -> Integer.compare(b.length(), a.length()));
			numericConstantsList = numericConstantsSet.toArray(new String[numericConstantsSet.size()]);
			Arrays.sort(numericConstantsList, (a, b) -> Integer.compare(b.length(), a.length()));
			

			// 3. 2nd pass through the file - We add DEFINES for C language keywords

			
			// We only need to include the DEFINES section after first #include, not after every #include
			boolean definesIncluded = false; 

			lineOfCode = null;
			String modifiedCodeline = null;
			fileReader = new FileReader(originalCodeFile);
			bufferedReader = new BufferedReader(fileReader);

			while ((lineOfCode = bufferedReader.readLine()) != null) {

				if (lineOfCode.startsWith("#include") && (!definesIncluded)) {
					stringBuffer.append(lineOfCode);
					stringBuffer = appendKeywordsDefines(stringBuffer);
					definesIncluded = true;
				} else {

					// If it's a line containing hardcoded strings
					if (lineOfCode.contains("\"")) {
						lineOfCode = obfuscateStringInCodeLine(lineOfCode);
					}

					// Replacing C keywords in the code with what is specified in the #define
					// section
					for (String key : defineKeywordsHashMap.keySet()) {
						if (lineOfCode.contains(key)) {
							lineOfCode = lineOfCode.replace(key, defineKeywordsHashMap.get(key));
						}

					}

					// 4. Removing the unnecessary spaces, the comments, replacing the variables and
					// obfuscating the hardcoded strings
					modifiedCodeline = removeTheSpaces(lineOfCode);
					modifiedCodeline = removeAllComments(modifiedCodeline);
					if (!modifiedCodeline.contains("#define")) {
						modifiedCodeline = replaceVariableAndFunctionNames(modifiedCodeline);

					}
					if (!modifiedCodeline.contains("\"")) {
						modifiedCodeline = replaceNumeric(modifiedCodeline);
					}

					stringBuffer.append(modifiedCodeline);
					// stringBuffer.append("\n");
				}

			}
			fileReader.close();

			// 5 Generating a truly random number in the range [10000, 1000000] 
			// And exporting the obfuscated code in the same directory as the original file

			String sourceFileName = originalCodeFile.getName();
			String sourceFilePath = originalCodeFile.getAbsolutePath();
			String pathToWrite = sourceFilePath.replace(sourceFileName, "");
			String nameWithoutExtension = sourceFileName.substring(0, sourceFileName.indexOf("."));
			int randomNumber = 0;

			try {
				randomNumber = getTrulyRandomNumber();
			} catch (IOException e) {
			} catch (InterruptedException e) {
			}
			String obfuscatedFileName = nameWithoutExtension + String.valueOf(randomNumber) + ".c";
			pathToWrite = pathToWrite + obfuscatedFileName;
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(pathToWrite)));

			// Write contents of stringBuffer to a file
			bufferedWriter.write(stringBuffer.toString());
			bufferedWriter.flush();
			bufferedWriter.close();

			System.out.println("Obfuscated source code file succesfully generated at --- " + pathToWrite);

		} else {
			System.out.println("Please pass the source code file path as argument");
		}

	}

	private static String replaceNumeric(String modifiedCodeline) {

		for (String key : defineNumericHashMap.keySet()) {

			if (modifiedCodeline.contains(key)) {

				modifiedCodeline = modifiedCodeline.replace(key, defineNumericHashMap.get(key));

			}
		}
		return modifiedCodeline;

	}

	private static String obfuscateStringInCodeLine(String codeLine) {

		// Looks for the content that is between " "
		Pattern pattern = Pattern.compile("(?<=\")(.*)(?=\")"); // something that is between " "

		Matcher matcher = pattern.matcher(codeLine);
		String lineBetweenApostrophe = null;

		matcher.find();

		lineBetweenApostrophe = codeLine.substring(matcher.start(), matcher.end());

		String lineBeforeApostrophe = codeLine.substring(0, codeLine.indexOf("\"") + 1);
		if (lineBeforeApostrophe.contains("printf")) {
			lineBeforeApostrophe = lineBeforeApostrophe.replace("printf", defineKeywordsHashMap.get("printf"));
		}

		lineBetweenApostrophe = obfuscateString(lineBetweenApostrophe);

		int closingApostropheIndex = codeLine.indexOf("\"", codeLine.indexOf("\"") + 1);
		String lineAfterApostrophe = codeLine.substring(closingApostropheIndex, codeLine.length());

		for (int i = 0; i < variablesAndFunctionsList.length; i++) {
			if (lineAfterApostrophe.contains(variablesAndFunctionsList[i])) {
				lineAfterApostrophe = lineAfterApostrophe.replace(variablesAndFunctionsList[i],
						String.join("", Collections.nCopies(3 * (i + 1), "a")));
			}
		}

		codeLine = lineBeforeApostrophe + lineBetweenApostrophe + lineAfterApostrophe;

		return codeLine;
	}

	static void identifyVarAndFuncNames(String lineOfCode) {

		// Regex demo -> https://regex101.com/r/m6Ugbo/1
		Pattern pattern = Pattern.compile("(?:(?<=void|int|char|double|float|short|long)(?:\\s+\\*?)"
				+ "([a-zA-Z_$][\\w$]*)(?:\\s*)|(?<=\\G,)(?:\\s*)"
				+ "([a-zA-Z_$][\\w$]*)(?:\\s*))(?=;|=|,|\\[|\\(|\\))");
		Matcher matcher = pattern.matcher(lineOfCode);
		while (matcher.find()) {

			String identifier = lineOfCode.substring(matcher.start(), matcher.end()).trim();
			// We skip the function main, it is a C keyword and will be defined later
			if (!identifier.equals("main")) {
				variablesAndFunctionsSet.add(identifier); // We add the variables in a set (we only need each element once)
			}
		}
	}

	static String removeTheSpaces(String lineOfCode) {
		if (lineOfCode.startsWith("#")) {
			return lineOfCode.concat("\n");
		} else {
			return lineOfCode.trim();
		}
	}

	static String removeAllComments(String codeLine) {
		String lineWithoutComment = codeLine;

		if (commentClosed == false)
			lineWithoutComment = "";

		if (codeLine.contains("//")) {
			int indexToDeleteFrom = codeLine.indexOf("//");
			lineWithoutComment = codeLine.substring(0, indexToDeleteFrom);

		}
		if (codeLine.startsWith("/*") && codeLine.endsWith("*/")) {
			lineWithoutComment = "";
			commentClosed = true;
		}
		if (codeLine.startsWith("/*") && !codeLine.contains("*/")) {
			commentClosed = false;
			int indexToDeleteFrom = codeLine.indexOf("/*");
			lineWithoutComment = codeLine.substring(0, indexToDeleteFrom);
		}
		if (!codeLine.startsWith("/*") && codeLine.endsWith("*/"))
			commentClosed = true;

		return lineWithoutComment;

	}

	static String replaceVariableAndFunctionNames(String codeLine) {
		// We relace each variable & function name, from the longest one to the shortest
		// with as many number of "a" as the index of that variable
		// in the sorted array of variables & function names
		
		for (int i = 0; i < variablesAndFunctionsList.length; i++) {

			if (codeLine.contains(variablesAndFunctionsList[i])) {
				codeLine = codeLine.replace(variablesAndFunctionsList[i],
						String.join("", Collections.nCopies(3 * (i + 1), "a")));
			}

		}

		return codeLine;

	}

	static String obfuscateString(String line) {
		// We transform every character into its ASCII counterpart, turn it to hex
		// and add "\x" before so it can be correctly displayed by the C compiler
		
		StringBuilder stringBuilder = new StringBuilder();
		char[] charArray = line.toCharArray();
		for (int i = 0; i < charArray.length; i++) {
			int asciiChar = (int) charArray[i];
			String hexAsciiChar = Integer.toHexString(asciiChar);
			stringBuilder.append("\\x");
			stringBuilder.append(hexAsciiChar);

		}

		return stringBuilder.toString();

	}

	static void showProgramIdentifiers() {
		// Identifiers = variable names & function names
		if (variablesAndFunctionsSet.isEmpty()) {
			System.out.println("No variables or functions identified yet!");
		} else {
			System.out.println("The variables are: ");
			for (int i = 0; i < variablesAndFunctionsList.length; i++) {
				System.out.println(variablesAndFunctionsList[i]);
			}
		}
	}

	static void identifyNumericCostants(String lineOfCode) {

		Pattern pattern = Pattern.compile("[0-9]+");

		Matcher matcher = pattern.matcher(lineOfCode);

		while (matcher.find()) {
			String identifiedNumber = lineOfCode.substring(matcher.start(), matcher.end()).trim();
			numericConstantsSet.add(identifiedNumber);
		}
	}

	static int getTrulyRandomNumber() throws IOException, InterruptedException {
		String postEndpoint = "https://api.random.org/json-rpc/4/invoke";

		// Generate random number between 10_000 and 1_000_000
		// https://api.random.org/json-rpc/4/request-builder
		// My API key from random.org is 1450ff6f-9cb3-4b78-bae7-8b6c9b7e83d0
		String inputJson = "{\"jsonrpc\":\"2.0\",\"method\":\"generateIntegers"
				+ "\",\"params\":{\"apiKey\":\"1450ff6f-9cb3-4b78-bae7-8b6c9b7e83d0"
				+ "\",\"n\":1,\"min\":1000,\"max\":10000000,\"replacement\":true,"
				+ "\"base\":10,\"pregeneratedRandomization\":null},\"id\":2699}";

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(postEndpoint))
				.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(inputJson))
				.build();

		HttpClient client = HttpClient.newHttpClient();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		String responseString = response.body();

		Pattern pattern = Pattern.compile("(?<=\\[)(.*)(?=\\])"); // what is between [ ]
		Matcher matcher = pattern.matcher(responseString);
		matcher.find();

		String randomNumberString = responseString.substring(matcher.start(), matcher.end());
		int randomNumber = Integer.parseInt(randomNumberString);

		return randomNumber;

	}

	private static StringBuffer appendKeywordsDefines(StringBuffer stringBuffer) {
		
		// We add the keyword-define corespondencies

                // We use a LinkedHashMap because its keeps the order in which keywords were added
		// Later, when we replace C langauge keyword, we want to replace "printf" before "int"
		// This is because "printf" containt "int" and we don't want to relace the "int" in "printf"
		// This is way it's important to keep printf before int in the HashMap
		
		String defineString, replacement, toReplace;
		String[] cLanguageKeyWords = { "auto", "continue", "extern", "volatile", "typedef", "union", "sizeof",
				"default", "struct", "switch", "void", "printf", "scanf", "int", "double", "float", "long", "char",
				"short", "signed", "unsigned", "static", "const", "if", "while", "for", "else", "do", "break", "case",
				"enum", "register", "goto", "return", "main", "getch" };

		List<String> linesWithDefines = new ArrayList<>();

		stringBuffer.append("\n");
		for (int i = 0; i < cLanguageKeyWords.length; i++) {
			replacement = String.join("", Collections.nCopies((i + 1), "_"));
			;
			defineString = "#define " + replacement + " " + cLanguageKeyWords[i];

			linesWithDefines.add(defineString);

			defineKeywordsHashMap.put(cLanguageKeyWords[i], replacement);
		}

		for (int i = 0; i < numericConstantsList.length; i++) {

			// stringBuffer.append("\n");
			replacement = String.join("", Collections.nCopies(3 * (i + 1), "b"));
			;
			defineString = "#define " + replacement + " " + numericConstantsList[i];
			linesWithDefines.add(defineString);

			toReplace = numericConstantsList[i];
			defineNumericHashMap.put(toReplace, replacement);
		}

		// We want to shuffle the #define lines randomly
		Collections.shuffle(linesWithDefines);
		for (String line : linesWithDefines) {
			stringBuffer.append(line);
			stringBuffer.append("\n");
		}

		// stringBuffer.append("\n");

		return stringBuffer;

	}

}
