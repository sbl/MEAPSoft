MEAPChunk {
	var <>fileName, <>segTime, <>segLength, <>edlTimes, <>featDict;
	
	*new { arg fileName, segTime, segLength;
		^super.newCopyArgs(fileName, segTime, segLength).init;
	}
	
	init {
		edlTimes = Dictionary.new;
		featDict = Dictionary.new;
	}
}

MEAPSoft {
	var <>server, <>outBus, <>group, <>edlList, <>fileNames, <>featDict, <>segList, <>chunkList, featInFileList;
	
	*new {arg server, outBus, group;
		^super.newCopyArgs(server, outBus, group).init;
	}
	
	init {
		fileNames = List.new;
		
		chunkList = List.new;
		segList = List.new;
		edlList = List.new(0);
		featDict = Dictionary.new;		
	}
	
	load {
		this.init;
		this.loadEDL;
		this.loadFEAT;
	}
	
	findChunk {arg chunkTemp;
		var ret;
		
		^chunkList.detect({arg item, i; 
			ret = ((item.fileName == chunkTemp.fileName)&&(item.segTime == chunkTemp.segTime)&&(item.segLength == chunkTemp.segLength))
		})
	}
	
	createSegList {
		segList=List.new;
		segList.addAll(chunkList);
		segList.sort({arg a, b; a.segTime<b.segTime});	
	}
	
	fillFeatDict {
		var featList;
		
		featInFileList.do{arg item, i;
			featList=List.new;
			featList.addAll(chunkList);
			featList.sort({arg a, b; 
				var ret;
				
				if(a.featDict[item[0].asSymbol].isFloat,{
					ret = (a.featDict[item[0].asSymbol]<b.featDict[item[0].asSymbol])
				},{
					ret = false
				});
				ret
			});
			featDict.add(item[0].asSymbol -> featList)
		}
	}
	
	addBufName { arg fileName;
		if(fileNames.indexOf(fileName.asSymbol)==nil,{
			fileNames.add(fileName.asSymbol);
		});
	}
	
	getFileName {arg string;
		var fileName;
		
		fileName = string.replace("%20"," ");
		this.addBufName(fileName);
		^fileName
	}
	
	loadEDL {
		var chunkTemp, line, temp, floats, floatCount, fileName, foundChunk, file;
	
		CocoaDialog.getPaths({ arg paths;
			paths.do({ arg p;

				file = File(p, "r");
				
				file.reset;
				2.do{line = file.getLine};
				while({line!=nil},{
					
					floats = List.new;
					
					line.asArray;
					
					floatCount = 0;
					line = line.split($ );
					//floats.add(line.removeAt(0));
//					line.do{|x| if(x.asFloat.asString.size>4, {floats.add(x); floatCount = floatCount+1}, {if (floatCount==0,{strings.add(x)})})};
					
					floats = [line[0],line[2],line[3]];
					
					fileName = this.getFileName(line[1]);
					
					chunkTemp = MEAPChunk(fileName.asSymbol, floats[1].asFloat, floats[2].asFloat);
					chunkTemp.edlTimes.add(edlList.size.asInteger -> floats[0].asFloat);
					foundChunk = this.findChunk(chunkTemp);
					if(foundChunk==nil,{
						chunkList.add(chunkTemp);
						edlList.add(chunkTemp);
					},{
						foundChunk.edlTimes.add(edlList.size.asInteger -> floats[0].asFloat);
						edlList.add(foundChunk);
					});
					line = file.getLine;
				});
				this.createSegList;
				file.close;
				"EDL Loaded".postln;
			})
		},{
			"cancelled".postln;
		}, 1);
	}
	
	loadSEG {
		var chunkTemp, foundChunk, floats, fileName, line, file;
		
		CocoaDialog.getPaths({ arg paths;
			paths.do({ arg p;

				file = File(p, "r");
				
				file.reset;
				3.do{line = file.getLine};
				while({line!=nil},{
					line.asArray;
					line = line.split($ );
					
					floats = [line[1],line[2]];
					
					fileName = this.getFileName(line[0]);
					
					this.addBufName(fileName);
					
					chunkTemp = MEAPChunk(fileName.asSymbol, floats[0].asFloat, floats[1].asFloat);
					
					foundChunk = this.findChunk(chunkTemp);
					if(foundChunk==nil,{
						chunkList.add(chunkTemp);
					});
				
					line = file.getLine;
				});
				this.createSegList;
				file.close;
				"SEG Loaded".postln;
			})
		},{
			"cancelled".postln;
		}, 1);
	
	}
	
	loadFEAT {
		var chunkTemp, foundChunk, floats, line, file, whereYat, numVals, segTime, segLength, fileName, featNums;
		
		CocoaDialog.getPaths({ arg paths;
			paths.do({ arg p;
				
				//get the features used
				file = File(p, "r");
				
				2.do{line = file.getLine};
				line = line.split($ );
				line = line.drop(2);
				line.do{arg item, i;
					whereYat = line[i].findBackwards(".");
					if(whereYat !=nil,{
						line.put(i,line[i].drop(whereYat+1));
					},{
						line.removeAt(i);
					})
				};
				featInFileList = List.new;
				line.do{arg item, i;
					whereYat = line[i].find("(");
					numVals = line[i].copyRange(whereYat+1, line[i].find(")")).asInteger;
					featInFileList.add([line[i].keep(whereYat), numVals]);
				};
				featInFileList.size;
				line = file.getLine(32767);
				while({line!=nil},{
					 
					line = line.split($ );
					
					fileName = this.getFileName(line.removeAt(0));
					
					this.addBufName(fileName);
					
					segTime = line.removeAt(0).asFloat;
					segLength = line.removeAt(0).asFloat;
					
					chunkTemp = MEAPChunk.new(fileName.asSymbol, segTime, segLength);
					
					featInFileList.do{arg item, i;
						featNums = line.keep(item[1].asInteger).asFloat;
						line = line.drop(item[1].asInteger);
						
						if(featNums.size == 1, {featNums = featNums[0]});
						
						chunkTemp.featDict.add(item[0].asSymbol -> featNums);
					};
					
					chunkTemp.featDict;
					
					foundChunk = this.findChunk(chunkTemp);
					if(foundChunk==nil,{
						chunkList.add(chunkTemp);
					},{
						foundChunk.featDict = chunkTemp.featDict;
					});
					
					line = file.getLine(32767);
				});
				this.createSegList;
				this.fillFeatDict;
				file.close;
				"FEAT Loaded".postln;
			})
		},{
			"cancelled".postln;
		}, 1);
	}
}