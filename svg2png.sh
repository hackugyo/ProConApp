#!/bin/bash

scalesvg ()
{
    svgfile="$1"
    pngdir="$2"
    pngscale="$3"
    qualifier="$4"

    svgwidthxheight=$(identify "$svgfile" | cut -d ' ' -f 3)
    svgwidth=${svgwidthxheight%x*}
    svgheight=${svgwidthxheight#*x}

    pngfile="$(basename $svgfile)" # Strip path.
    pngfile="${pngfile/.svg/.png}" # Replace extension.
    pngfile="${pngfile/[^A-Za-z0-9._]/_}" # Replace invalid characters.
    pngfile="$pngdir/$qualifier/$pngfile" # Prepend output path.

    if [ ! -d $(dirname "$pngfile") ]; then
        echo "WARNING: Output directory does not exist: $(dirname "$pngfile")"
        #echo "Exiting"
        #exit 1
        echo "Outputting here instead: $pngfile"
        pngfile="$qualifier-${svgfile/.svg/.png}"
    fi

    pngwidth=$(echo "scale=0; $svgwidth*$pngscale" | bc -l)
    pngheight=$(echo "scale=0; $svgheight*$pngscale" | bc -l)
    pngdensity=$(echo "scale=0; 72*$pngscale" | bc -l) # 72 is default, 

    echo "$svgfile ${svgwidth}×${svgheight}px -> $pngfile ${pngwidth}×${pngheight}px @ $pngdensity dpi"

    convert -background transparent -density $pngdensity "$svgfile" "$pngfile"
    #inkscape -w${pngwidth} --export-background-opacity=0 --export-png="$pngfile" "$svgfile" > /dev/null
    #convert "$svgfile" -background transparent -scale ${pngwidth}x${pngheight} "$pngfile"
}



svgfiles="$1"
svgfiles="${svgfiles:=*.svg}" # Default to input all *.svg in current dir.

pngdir="$2"
pngdir="${pngdir:=../res}" # Default to place output pngs to ../res, ie. ../res/drawable-hdpi etc.

for svgfile in $svgfiles; do
    echo "Scaling $svgfile ..."
    scalesvg "$svgfile" "$pngdir" 0.75 drawable-ldpi
    scalesvg "$svgfile" "$pngdir" 0.48    drawable-mdpi
    scalesvg "$svgfile" "$pngdir" 0.72  drawable-hdpi
    scalesvg "$svgfile" "$pngdir" 0.96    drawable-xhdpi
    scalesvg "$svgfile" "$pngdir" 1.44    drawable-xxhdpi
    scalesvg "$svgfile" "$pngdir" 1.92    drawable-xxxhdpi
done

echo -n "Done."
read # I've made it wait for Enter -- convenient when run from Nautilus.
