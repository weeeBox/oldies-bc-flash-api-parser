package bc.actionscript;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bc.code.ListWriteDestination;
import bc.code.WriteDestination;

public class ApiReader
{
	static Pattern packagePattern = Pattern.compile("<tr><td class=\"classHeaderTableLabel\">Package</td><td><a.*?>([\\w\\d_\\.]+)</a></td></tr>");
	static Pattern classPattern = Pattern.compile("<tr><td class=\"classHeaderTableLabel\">Class</td><td class=\"classSignature\">(.*?)</td></tr>");
	
	static Pattern dataPattern = Pattern.compile("<div class=\"detailBody\"><code>(.*?)</code>");
	static Pattern hrefPattern = Pattern.compile("<a href=\".*?\">(.*?)</a>");
	static Pattern propertryPattern = Pattern.compile("([\\w\\d_\\.]+):([\\w\\d_\\.\\*]+)");
	
	static Pattern superClassPattern = Pattern.compile("<td class=\"inheritanceList\">(.*?)</td>");
	static Pattern classHrefPattern = Pattern.compile("<a href=\"\\.\\./\\.\\./([\\w\\d_/]+)\\.html\">([\\w\\d_\\.]+)</a>");
	
	static Map<String, String> typeLookup;
	static
	{
		typeLookup = new HashMap<String, String>();
		typeLookup.put("Number", "float");
		typeLookup.put("Boolean", "bool");
		typeLookup.put("*", "Object");
		typeLookup.put("Array", "AsArray");
	}
	
	public static void main(String[] args) throws Exception
	{
		String data = getText(args[0]);
		
		Matcher dataMatcher = dataPattern.matcher(data);
		
		List<WriteDestination> functions = new ArrayList<WriteDestination>();
		List<WriteDestination> fields = new ArrayList<WriteDestination>();
		List<WriteDestination> property = new ArrayList<WriteDestination>();
		
		while (dataMatcher.find())
		{
			String funcString = dataMatcher.group(1);
			Matcher hrefMatcher = hrefPattern.matcher(funcString);
			while (hrefMatcher.find())
			{
				funcString = funcString.replace(hrefMatcher.group(), hrefMatcher.group(1));
			}
			
			ListWriteDestination dest = new ListWriteDestination("    ");
			
			Matcher propertyMatcher = propertryPattern.matcher(funcString);
			if (propertyMatcher.matches())
			{
				String name = propertyMatcher.group(1);
				String type = propertyMatcher.group(2);
				
				dest.writef("public function get %s():%s", name, type);
				dest.writeln(" { throw new NotImplementedError(); }");
				
				property.add(dest);
				
				dest = new ListWriteDestination("    ");
				dest.writef("public function set %s(value:%s):void", name, type);
				dest.writeln(" { throw new NotImplementedError(); }");
				
				property.add(dest);
			}
			else
			{
				if (funcString.contains("function"))
				{
					dest.writelnf("%s", funcString);
					dest.writeBlockOpen();
					dest.writeln("throw new NotImplementedError();");
					dest.writeBlockClose();
					
					functions.add(dest);
				}
				else
				{
					dest.writelnf("%s;", funcString);
					fields.add(dest);
				}
			}
		}
		
		String packageName = "bc.flash";
		
		Matcher packageMatcher = packagePattern.matcher(data);
		if (packageMatcher.find())
		{
			packageName = "bc." + packageMatcher.group(1);
		}
		
		String classDecl = null;
		Matcher classMatcher = classPattern.matcher(data);
		if (classMatcher.find())
		{
			classDecl = classMatcher.group(1).replace("  ", " ");
		}
		
		List<String> imports = new ArrayList<String>();
		
		String superClass = null;
		Matcher superClassMatcher = superClassPattern.matcher(data);
		if (superClassMatcher.find())
		{
			String superClassData = superClassMatcher.group(1);
			Matcher hrefMatcher = classHrefPattern.matcher(superClassData);
			
			if (hrefMatcher.find())
			{
				imports.add("bc." + hrefMatcher.group(1).replace('/', '.'));
				superClass = hrefMatcher.group(2);
			}
		}
		
		imports.add("bc.flash.errors.NotImplementedError");
		Collections.sort(imports);

		ListWriteDestination dest = new ListWriteDestination("    ");
		
		dest.writelnf("package %s", packageName);
		dest.writeBlockOpen();
		
		for (String string : imports)
		{
			dest.writelnf("import %s;", string);
		}
		
		if (imports.size() > 0) dest.writeln();
		
		dest.writeln(classDecl + " : " + superClass);
		dest.writeBlockOpen();
		
		for (Object string : fields)
		{
			dest.writeln(string);
		}
		
		if (fields.size() > 0) dest.writeln();
		
		for (Object string : functions)
		{
			dest.writeln(string);
			dest.writeln();
		}
		
		for (Object string : property)
		{
			dest.writeln(string);
		}
		
		dest.writeBlockClose();
		dest.writeBlockClose();
		
		System.out.println(dest);
	}

	public static String getText(String url) throws Exception {
        URL website = new URL(url);
        URLConnection connection = website.openConnection();
        BufferedReader in = new BufferedReader(
                                new InputStreamReader(
                                    connection.getInputStream()));

        StringBuilder response = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null) 
            response.append(inputLine);

        in.close();

        return response.toString();
    }
}
