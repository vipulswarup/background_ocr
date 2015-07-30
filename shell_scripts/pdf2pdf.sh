timestamp="$(date -u +%N)"
#echo $timestamp
tempfolder=/tmp/tesseract_temp_$timestamp
echo $tempfolder

mkdir $tempfolder
convert           \
   -density 200  \
    $1      \
    $tempfolder/temp_$timestamp.jpg

jpgCount=$(ls -1 $tempfolder | wc -l)
echo $jpgCount

if [ "$jpgCount" -gt "1" ]; then
	#if page count >1, use parallel to run tesseract and then assemble into target PDF
	parallel --gnu "tesseract {} {.} pdf" ::: $tempfolder/*.jpg
	pdfunite $tempfolder/*.pdf $2.pdf 2>/tmp/Alfresco/pdfunite.out
else
	if [ "$jpgCount" -eq "1" ]; then
		#if there is only 1 page, run simple tesseract and copy output to target file
		cd "$tempfolder"
		currentFolder=$(pwd)
		echo "currently in"		
		echo $currentFolder
		echo "Source file = temp_"+$timestamp+".jpg"
		sourceJPG=temp_$timestamp.jpg
		sourceTIFF=temp_$timestamp.tiff
		targetPDF=temp_out_$timestamp
		#convert $sourceJPG $sourceTIFF
		tesseract $sourceJPG $targetPDF pdf 
		currentFolder=$(pwd)
		echo "currently in"		
		echo $currentFolder
		ls -al
		cp $targetPDF.pdf $2
	else
		echo "found 0 files"
		exit 1
	fi
fi


rm -rf $tempfolder

exit 0


