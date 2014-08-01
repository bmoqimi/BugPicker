var privateNum = 1;
var publicNum = 1;
var ProtectedNum = 1;
var StaticNum = 1;
var FinalNum = 1;
var SynchronizedNum = 1;
var BridgeNum = 1;
var VarargsNum = 1;
var NativeNum = 1;
var AbstractNum = 1;
var StrictNum = 1;
var tempText="";


function FlagFilter(tagee) {

		switch (tagee) 
		{
			case 'private':
				privateNum = privateNum *-1; 
				break;
			case 'public':
				publicNum = publicNum *-1;
				break;
			case 'protected':
				ProtectedNum = ProtectedNum *-1;
				break;
			case 'static':
				StaticNum = StaticNum *-1;
				break;
			case 'final':
				FinalNum = FinalNum *-1;
				break;
			case 'synchronized':
				SynchronizedNum = SynchronizedNum *-1;
				break;
			case 'bridge':
				BridgeNum = BridgeNum *-1;
				break;
			case 'varargs':
				VarargsNum = VarargsNum *-1;
				break;
			case 'native':
				NativeNum = NativeNum *-1;
				break;
			case 'abstract':
				AbstractNum = AbstractNum *-1;
				break;
			case 'strict':
				StrictNum = StrictNum *-1;
				break;
		}
			
			$( "div[class*='methodinfo']" ).hide();  
			
			if(privateNum+publicNum+ProtectedNum+StaticNum+FinalNum+SynchronizedNum+BridgeNum+VarargsNum+NativeNum+AbstractNum+StrictNum ==11 || privateNum+publicNum+ProtectedNum+StaticNum+FinalNum+SynchronizedNum+BridgeNum+VarargsNum+NativeNum+AbstractNum+StrictNum == -11)
			{
				$( "div[class*='methodinfo']" ).show(); 	
			}
			else
			{
				var conditionString="div[class*='methodinfo']";
				if(privateNum ==-1)
				{
					conditionString=conditionString+"[name*='private']";
				}
				if(publicNum ==-1)
				{
					conditionString=conditionString+"[name*='public']";
					
				}
				if(ProtectedNum ==-1)
				{
					conditionString=conditionString+"[name*='protected']";
				}
				if(StaticNum ==-1)
				{
					conditionString=conditionString+"[name*='static']";
				}
				if(FinalNum ==-1)
				{
					conditionString=conditionString+"[name*='final']";
				}
				if(SynchronizedNum ==-1)
				{
					conditionString=conditionString+"[name*='synchronized']";
				}
				if(BridgeNum ==-1)
				{
					conditionString=conditionString+"[name*='bridge']";
				}
				if(VarargsNum ==-1)
				{
					conditionString=conditionString+"[name*='varargs']";
				}
				if(NativeNum ==-1)
				{
					conditionString=conditionString+"[name*='native']";
				}
				if(AbstractNum ==-1)
				{
					conditionString=conditionString+"[name*='abstract']";
				}
				if(StrictNum ==-1)
				{
					conditionString=conditionString+"[name*='strict']";
				}
				$( conditionString ).show();
			}
			if(tempText!="")
			$( "div[class*='methodinfo']" ).not("div[title*='"+tempText+"']").hide(); 
}

function NameFilter(text) 
{
	tempText=text;
	FlagFilter("none");
	if(text!="")
	$( "div[class*='methodinfo']" ).not("div[title*='"+text+"']").hide(); 
}