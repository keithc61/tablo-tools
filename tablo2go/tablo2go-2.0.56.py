#!/usr/bin/env python

# TabloExtractor, J. Kenney 2014
#  https://sites.google.com/a/moboard.com/tablo/
#  A script to pull videos off of the TabloTV OTA Recorder
#  Feel free to modify, and distribute as needed, but ensure
#  that any updated scripts include:
#  - Author and supporting people's information/contributions
#  - Embed any links where you found useful information.

# Recommended Programs to install

# FFMPEG
#  http://www.ffmpeg.org/download.html
#  Without this, only .ts files can be created
#  REQUIRED for conversions to .mp4, tagging, subtitles

# CCExtractor
#  http://ccextractor.sourceforge.net/download-ccextractor.html
#  REQUIRED for subtitle extraction

# Mutagen python module
#  https://bitbucket.org/lazka/mutagen
#  Will allow for itunes like metadata tagging
#  REQUIRED for metadata tagging

# Handbrake
#  https://handbrake.fr/downloads.php
#  This is a depricated feature that should be removed
#  No tagging or subtitle support with this.
#  REQUIRED for .mkv files and re-encoding

# Future TODO (V3+)
# - Allow max length settings per metadata item (cut if too long)
# - Cut a portion of a video if too long
# - Delete Videos after Download (using tablo api)
# - Comskip
# - GUI

# Known Issues
# - If the file already exists in the exists dir, then it stays in the temp directory
#   (I can't think of a good solution for this...)
# - Metadata looks correct when viewed via AtomicParsley but does not look correct in itunes

# Special thanks to M. Tuckman for making everything better
#  https://github.com/miketuckman/TabloExtract
# And, mjarends for help with getting video files
#  https://bitbucket.org/mjarends/tablo-scripts/src/d6f8a23744490896bc884cb9c3bac0307a7dc23b?at=master
# And, cberry for the making the finding of tablos possible!
#  http://pastebin.com/7Fu6DKM7

# Based on information obtained at:
#  1. http://community.tablotv.com/discussion/226/can-i-pull-recorded-video-files-off-tablo
#  2. http://stackoverflow.com/questions/22676/how-do-i-download-a-file-over-http-using-python
#  3. http://stackoverflow.com/questions/1191374/subprocess-with-timeout
#  4. http://thraxil.org/users/anders/posts/2008/03/13/Subprocess-Hanging-PIPE-is-your-enemy/

# Note: Search string no longer needs to be in quotes.
# Note: Order of arguments makes no difference

# Example 1. Process all new episodes of The Simpsons
#    python tablo2.py The Simpsons
# Example 2. Process a video with a specific video id (Tablo ID), will search all tablos for this
#    python tablo2.py 121545
# Example 3. Process a video with a specific tmsid, will search all tablos for this
#    python tablo2.py EP019223320011
# Example 4. Mark all episodes of The Simpsons as complete (will not be downloaded in the future)
#    python tablo2.py The Simpsons -complete
# Example 5. List all unprocessed (undownloaded) episodes of The Simpsons
#    python tablo2.py The Simpsons -list
# Example 6. List all episodes of The Simpsons (regardless of having been downloaded before)
#            And provide additional information about the episodes
#    python tablo2.py -list -long -ignore The Simpsons
# Example 7. Download all unprocessed episodes of The Simpsons and put them in a directory
#            Structure like /share/TV/The Simpsons/Season 2/
#    python tablo2.py -output:/share/TV The Simpsons
# Example 8. Download all unprocessed videos, but put movies somewhere else
#    python tablo2.py -output:/share/TV -moviedir:/share/Movies
# Example 9. Download all unprocessed videos, but put movies somewhere else, and failed (no metadata)
#            Videos in a separate directory for later processing.
#    python tablo2.py -output:/share/TV -moviedir:/share/Movies -faildir:/share/Unknown
# Example 10.List all videos on a specific tablo(s)
#    python tablo2.py -list -tablo:192.168.1.55
#    python tablo2.py -list -tablo:192.168.1.55:192.168.1.56
# Example 11.A Problem with metadata, nothing I can do...  There are two S01E175's
#            and EP017211070272 has no metadata - this one will be marked in kmttg format
#            and placed into the "faildir" for later processing, the second S01E175 will be placed in
#            "existdir".
#    python tablo2.py -ignore -test -list Fallon
#    EP017211070267 The Tonight Show Starring Jimmy Fallon -   - S01E172
#    EP017211070269 The Tonight Show Starring Jimmy Fallon -   - S01E174
#    EP017211070271 The Tonight Show Starring Jimmy Fallon -   - S01E176
#    EP017211070270 The Tonight Show Starring Jimmy Fallon -   - S01E175
#    EP017211070272 The Tonight Show Starring Jimmy Fallon -   -
#    EP017211070273 The Tonight Show Starring Jimmy Fallon -   - S01E175

# Below are all of the configurable options, you can edit the defaults here.
# Run the command with -help to see your settings

# TYPE      OPTION      DEFAULT                 INPUT     DESCRIPTION OF FEATURE
#                       '*' = REQUIRED!
#                       EDIT ONLY THIS COLUMN
# ------    ---------   ---------------------   --------  ---------------------------------------------------------
OPTIONS  = {'tablo':    ['auto',                'IPADDR', 'Tablo IP address'],
            'db':       ['ignore',              'PATH',   'Use a cacheing database'],
            'dbtime':   [604800,                'TIME',   'Time where a cached entry is valid'],
            'tv':       [False,                 '',       'Process only TV shows'],
            'movies':   [False,                 '',       'Process only Movies'],
            'sports':   [False,                 '',       'Process only Sports'],
            'tvcreate': [True,                  '',       'Create Show/Season X directories'],
            'tvdir':    ['./tv',                'PATH',   'Save TV shows here'],
            'faildir':  ['./fail',              'PATH',   'Location to save unknown tv videos'],
            'moviedir': ['./movies',            'PATH',   'Save movies to this directory'],
            'sportsdir':['./sports',            'PATH',   'Save sports shows to this directory'],
            'tempdir':  ['./',                  'PATH',   'Location of temp directory'],
            'existdir': ['./exists',            'PATH',   'Location to move duplicate files'],
            'ffmpeg':   ['./ffmpeg.exe',        'PATH',   'Path to ffmpeg'],
            'handbrake':['./HandBrakeCLI.exe',  'PATH',   'Path to HandBrakeCLI'],
            'ccextract':['./ccextractorwin.exe','PATH',   'Path to ccextractor'],
            'cc':       [False,                 '',       'Embed Closed Captioning into file'],
            'noescape': [True,                  '',       'Forbid the \\ character in paths'],
            'mce':      [False,                 '',       'Save TV shows in MCEBuddy format'],
            'mp4':      [True,                  '',       'Use .mp4 (requires ffmpeg)'],
            'mp4tag':   [True,                  '',       'Tag with metadata (requires mutagen)'],
            'mkv':      [False,                 '',       'Use .mkv (requires HandBrakeCLI)'],        
            'history':  ['tablo.history',       'PATH',   'File to save history data to'],
            'kmttg':    ['auto.history',        'PATH',   'Kmttg history file (optional)'],
            'ignore':   [False,                 '',       'Ignore history files'],
            'complete': [False,                 '',       'Mark matching videos as downloaded'],
            'a':        [False,                 '',       'Reprocess continually (see -sleep)'],
            'sleep':    [1800,                  'TIME',   'Number of seconds to sleep'],
            'summary':  [False,                 '',       'Display summary information only'],
            'list':     [False,                 '',       'List matching videos on Tablo(s)'],
            'long':     [False,                 '',       'Expand list view with more detail'],
            'help':     [False,                 '',       'Show this help screen'],
            'test':     [False,                 '',       'Test System - do not download/mark'],
            'debug':    [True,                  '',       'Show all debugging output'],
            'delay':    [0,                     'TIME',   'Shows must be x seconds old'],
            'not':      [False,                 '',       'Invert the selection'],
            'custom':   ['',                    'NAME',   'Custom File Naming'],
            'log':      ['tablo.log',           'PATH',   'Log messages to a file (or use off)']}

# There are a few hidden options as well, this could be added above to make things neater
# but I didnt want the help screen to be even longer
#  -reallylong shows all metadata retrieved from the tablo (these fields are valid in custom)
#  -customtv allows for custom tv naming
#  -custommovie allows for custom movie naming
#  -customsports allows for custom sports naming
#  -skipdelete will not delete .ts file
#  -skipdownload will not download the files (useful in testing if used with -skipdelete)

VERSION = "2.0.56"

global debugging, infinity_timeout, true, false, LOGFILE, tagging
debugging = 1
infinity_timeout = 0
true, false = 1, 0
tagging = False

import os,sys,string,time,urllib,re,subprocess,urllib2,shutil
from calendar import timegm
from datetime import datetime

try:
    from mutagen.mp4 import MP4
    tagging = True
except:
    tagging = False

##########################################
###  File and Data Structure Functions ###
##########################################

#################################################################################################
# {} = getDB(filename_with_odb)
def getDB(file_DB):
    try:
        results = eval(open(file_DB).readlines()[0])
    except:
        results = {}
    return results

#################################################################################################
# Save a {} to a file
def writeDB(file_DB, data):
    try:
        file = open(file_DB, 'w')
        file.write(str(data)+'\n')
        file.close()
    except:
        debug('Unable to write file '+str(file_DB))

#################################################################################################
# return a value in a dict
def rDict(_dict, _default, *sequence):
    if not sequence:
        return _default
    for item in sequence:
        if not _dict.has_key(item):
            return _default
        _dict = _dict[item]
    return _dict

#################################################################################################
# return a value in a dict as a list (if not already a list)
def lDict(_dict, _default, *sequence):
    results = sDict(_dict, _default, sequence)
    if str(type(results)) != "<type 'list'>":
        results = [results]
    return results

#################################################################################################
# Return the first item in a list if a list
def sList(_list):
    if str(type(_list)) == "<type 'list'>":
        return str(_list[0])
    else:
        return str(_list)

#################################################################################################
# convert a metadata formated dictionary to a normal dictionary
def convert_meta(DICT, *loc):
    results = {}
    _path = ''
    if loc:
        _path = loc[0]
    if str(type(DICT)) != "<type 'dict'>":
        if str(type(DICT)) != "<type 'list'>":
            results[_path] = str(DICT)
        else:
            for i in range(len(DICT)):
                if _path == '':
                    x = convert_meta(DICT[i], str(i))
                else:
                    x = convert_meta(DICT[i], _path+'.'+str(i))
                if x != {}:
                    for j in x.keys():
                        results[j] = x[j]
    else:
        keys = DICT.keys()
        keys.sort()
        for key in keys:
            if _path == '':
                x = convert_meta(DICT[key], key)
            else:
                x = convert_meta(DICT[key], _path+'.'+key)
            if x != {}:
                for i in x.keys():
                    results[i] = x[i]
    return results

#################################################################################################
# convert an OPTIONS formated dictionary to a normal dictionary
def convert(OPTIONS):
    results = {}
    for k in OPTIONS.keys():
        results[k] = OPTIONS[k][0]
    return results

#################################################################################################
# allow for custom find and replace
def custom(line, *DICTS):
    for d in DICTS:
        for k in d.keys():
            if string.find(line, '{'+str(k)+'}') != -1:
                tmp = string.splitfields(line, '{'+k+'}')
                line = str(d[k]).join(tmp)
    return line

#################################################################################################
# Clean a string of all bad characters
# Default ASCII ranges 48-57, 65-90, 97-122, - _ . are allowed,
#  otherwise BAD_CHARS can be defined directly
def clean(input, *OVERRIDE):
    result = ''
    BAD_CHARS = {'(':'(', ')':')', ' ':' ', '"':' ', '&':'+', '/':' ', '\\':' ', '|':' ', "'":"", '?':'', u'\u2026':'', '@':'at ', u'\u2019':'',u'\xf8':''}
    if OVERRIDE:
        OVERRIDE = OVERRIDE[0]
        keys = OVERRIDE.keys()
        for key in keys:
            BAD_CHARS[key] = OVERRIDE[key]
    lastchar = 'Z*Z'
    for char in input:
        o = ord(char)
        if (o >= 48 and o <= 57) or (o >= 65 and o <= 90) or (o >= 97 and o <= 122) or (o == 95) or (o == 46) or (o == 45) or BAD_CHARS.has_key(char):
            if BAD_CHARS.has_key(char):
                char = BAD_CHARS[char]
            if lastchar != ' ' or char != ' ':
                result = result + char
            lastchar = char
    return result

#################################################################################################
# Reduct a dict to having only string values
def cleanmeta(ind):
    results = {}
    keys = ind.keys()
    keys.sort()
    for key in keys:
        try:
            value = ind[key]
            if str(type(value)) == "<type 'list'>":
                value = value[0]
            value = clean(str(value))
            t = str(type(value))
            if t == "<type 'int'>" or t == "<type 'float'>":
                value = str(value)
                results[key] = value
            elif t == "<type 'str'>":
                #value = '"'+value+'"'
                results[key] = value
        except:
            error = 1
    return results

#################################################################################################
# Function to see the recorded id's of shows that have already been processed
# Using the same format as kmttg's auto.history
def get_history(filename, *oldtable):
    transfered = {}
    if oldtable:
        transfered = oldtable[0]
    file_data = []
    try:
        file_data = open(filename).readlines()
    except:
        file_data = []
    for line in file_data:
        line = string.strip(line)
        eid = string.split(line)[0]
        transfered[eid] = line
    return transfered

#################################################################################################
# Function to write the recorded id's of shows that have just been processed
# Using the same format as kmttg's auto.history
def write_history(filename, vidtype, eid, series, title, name):
    try:
        if vidtype != 'movie':
            entry = eid + ' ' + series + ' - ' + title
        else:
            entry = eid + ' ' + name
        if filename != 'off':
            log = open(filename, 'a')
            log.write(entry+'\n')
            log.close()
    except:
        debug('FAIL: unable to write to history file')

#################################################################################################
# used for all debugging
def debug(output, *second):
    if second:
        output = '['+time.asctime(time.localtime(time.time()))[4:]+'] [' +second[0]+ '] - '+str(output)
    else:
        output = '['+time.asctime(time.localtime(time.time()))[4:]+'] - '+str(output)
    if debugging:
        print(output)
    if LOGFILE != 'off':
        try:
            log = open(LOGFILE, 'a')
            log.write(output+'\n')
            log.close()
        except:
            error = 1

#################################################################################################
# Move a file to a specific directory (creating as necessary) and rename it
def move_file(filein, newpath, filename, existdir):
    filename = filename + '.'+string.splitfields(filein, '.')[-1]
    fpath = string.splitfields(newpath, '/')
    npath = ''
    for directory in fpath:
        npath = npath + directory
        if npath != '.':
            try:
                os.mkdir(npath)
            except:
                ohwell = 1
        npath = npath + '/'
    try:
        os.stat(npath+filename)
        fpath = string.splitfields(existdir, '/')
        npath = ''
        for directory in fpath:
            npath = npath + directory
            if npath != '.':
                try:
                    os.mkdir(npath)
                except:
                    ohwell = 1
            npath = npath + '/'
    except:
        ohwell = 1
    filename = npath+filename
    try:
        shutil.move(filein, filename)
    except:
        debug('Unable to move file, keeping it in tempdir')

##########################################
###    Command Line Fuctions           ###
##########################################

#################################################################################################
# process command line arguments, takes a {} as input
# format of input {'commandline': [default, inputexample, description]}
# if default = '*' then it is a required option.
def get_cmd_line(*INPUT):
    OPTIONS = {}
    FLAGS = {}
    if INPUT:
        OPTIONS = INPUT[0]
    EXTRA = []
    ERROR = False
    for opt in sys.argv[1:]:
        if opt[0] == '-':
            opt = opt[1:]
            value = True
            if string.find(opt, ':') != -1:
                tmp = string.splitfields(opt, ':')
                opt = tmp[0]
                value = tmp[1:]
                if len(value) == 1:
                    value = value[0]
            if not OPTIONS.has_key(opt):
                OPTIONS[opt] = [value, '', 'Manually added']
            else:
                OPTIONS[opt][0] = value
        else:
            EXTRA.append(opt)
    EXTRA = ' '.join(EXTRA)
    OPTIONS['{}'] = [EXTRA, '', 'Search Regex']
    # Search for missing options
    for opt in OPTIONS.keys():
        if OPTIONS[opt][0] == '*':
            ERROR = True
        if OPTIONS[opt][0] == True:
            FLAGS[opt] = True
    if OPTIONS.has_key('help') and OPTIONS['help'][0]:
        ERROR = True
    # Search for windows drive letters
    for opt in OPTIONS.keys():
        if OPTIONS[opt][1] == 'PATH':
            if str(type(OPTIONS[opt][0])) == "<type 'list'>":
                OPTIONS[opt][0] = ":".join(OPTIONS[opt][0])
            if FLAGS.has_key('noescape'):
                value = ''
                for char in str(OPTIONS[opt][0]):
                    if char == '\\':
                        value = value + '/'
                    else:
                        value = value + char
                OPTIONS[opt][0] = value
    # Print 
    if ERROR:
        print('Tablo Extractor (Version '+str(VERSION)+')')
        print('Usage: '+sys.argv[0]+' <options> "search regex"')
        print('')
        print('  '+string.ljust('OPTION', 16) + string.ljust('DESCRIPTION',36) + 'VALUE')
        print('  --------------- ----------------------------------- -------------------')
        FOUND_REQ = False
        if OPTIONS != {}:
            o = OPTIONS.keys()
            o.sort()
            for key in o:
                tmp = OPTIONS[key]
                req = ' '
                value = tmp[0]
                if value == 0:
                    value = 'False'
                elif value == 1:
                    value = 'True'
                elif value == '*':
                    req = '*'
                    FOUND_REQ = True
                if tmp[1] == '':
                    print(' '+req+'-'+string.ljust(key, 15) + string.ljust(tmp[2],36) + str(value))
                else:
                    print(' '+req+'-'+string.ljust(key+':'+tmp[1], 15) + string.ljust(tmp[2],36) + str(value))
        if FOUND_REQ:
            print(' * marks required options/flags')
        sys.exit()
    return OPTIONS, FLAGS

##########################################
###    Tablo Functions                 ###
##########################################

#################################################################################################
# Function to get a list of your tablos (Many thanks to cberry on the tablo community forums)
def get_tablos():
    results = {}
    try:
        resp = urllib2.urlopen("https://api.tablotv.com/assocserver/getipinfo/").read()
        resp = rDict(eval(resp), [], 'cpes')
        for line in resp:
            tablo_ip = rDict(line, '', 'private_ip')
            if tablo_ip != '':
                results[tablo_ip] = line
    except KeyboardInterrupt:
        debug('Exiting during tablo search')
        sys.exit()
    except:
        debug('Unable to automatically locate Tablos')
    return results

#################################################################################################
# Function to get a list of video id's from a tablo - use pvr directory to get ids
# This will retrieve the list of video ids by parsing the directory names
# Version 2.0 - Format changed, returns a simple list of ints.
def get_list(IPADDR):
    results = []
    try:
        resp = urllib.urlopen('http://'+IPADDR+':18080/pvr').read()
        resp = string.splitfields(resp, '\n')
        for line in resp:
            if string.find(line, '<tr><td class="n"><a href="') == 0:
                line = string.splitfields(line, '<tr><td class="n"><a href="')[1]
                if line[0] != '.':
                    line = eval(string.splitfields(line, '/')[0])
                    results.append(line)
        results.sort()
    except KeyboardInterrupt:
        debug('Exiting during Tablo listing')
        sys.exit()
    except:
        debug('FAIL: Unable to download file list',str(IPADDR))
    return results

#################################################################################################
# Function to get a metadata from a videoid from a specific tablo
def get_meta(IPADDR, VIDEOID):
    try:
        resp = urllib2.urlopen('http://'+IPADDR+':18080/pvr/'+str(VIDEOID)+'/meta.txt').read()
        temp = ''
        for i in range(len(resp)):
            temp = temp + resp[i]
        temp = eval(temp)
        temp['cache'] = int(time.time())
        return temp
    except KeyboardInterrupt:
        debug('Exiting during metadata download')
        sys.exit()
    except:
        return {}

#################################################################################################
# Get a value from a dictionary via an input like "a.b.c.d.e"
def get_value(DICT, VKEYS, DEFAULT):
    if str(type(DICT)) != "<type 'dict'>":
        return DEFAULT
    key_top = string.splitfields(VKEYS, '.')[0]
    if not DICT.has_key(key_top):
        return DEFAULT
    key_bottom = ''
    key_index = string.find(VKEYS, '.')
    if key_index != -1:
        key_bottom = VKEYS[key_index+1:]
    if key_bottom == '':
        return DICT[key_top]
    else:
        return get_value(DICT[key_top], key_bottom, DEFAULT)

#################################################################################################
# Get primary metadata fields (series name, episode name, season number, episode number, etc)
def proc_meta(metadata, *OPTIONS):
    if OPTIONS:
        OPTIONS = OPTIONS[0]
    else:
        OPTIONS = {}
    PROC = {}
    PROC['cache']    = get_value(metadata, 'cache', 0)

    PROC['end']      = get_value(metadata, 'recSportEvent.jsonFromTribune.endTime', '2014-01-01T09:00Z')
    PROC['end']      = get_value(metadata, 'recEpisode.jsonFromTribune.endTime', PROC['end'])
    PROC['end']      = get_value(metadata, 'recMovieAiring.jsonFromTribune.endTime', PROC['end'])
    PROC['type']     = get_value(metadata, 'recSportEvent.jsonFromTribune.program.entityType', 'manual')
    PROC['type']     = get_value(metadata, 'recMovieAiring.jsonFromTribune.program.entityType', PROC['type'])
    PROC['status']   = get_value(metadata, 'recManualProgramAiring.jsonForClient.video.state','unknown')
    PROC['status']   = get_value(metadata, 'recSportEvent.jsonForClient.video.state',PROC['status'])
    PROC['status']   = get_value(metadata, 'recMovieAiring.jsonForClient.video.state',PROC['status'])
    PROC['quality']  = get_value(metadata, 'recManualProgramAiring.jsonForClient.video.height', '')
    PROC['quality']  = get_value(metadata, 'recSportEvent.jsonForClient.video.height', PROC['quality'])
    PROC['quality']  = get_value(metadata, 'recMovieAiring.jsonForClient.video.height', PROC['quality'])
    PROC['airdate']  = get_value(metadata, 'recManualProgramAiring.jsonForClient.airDate', '')
    PROC['airdate']  = get_value(metadata, 'recSportEvent.jsonForClient.airDate', PROC['airdate'])
    PROC['airdate']  = get_value(metadata, 'recMovieAiring.jsonForClient.airDate', PROC['airdate'])
    PROC['airdate']  = get_value(metadata, 'recMovie.jsonFromTribune.releaseYear', PROC['airdate'])
    PROC['desc']     = get_value(metadata, 'recSportEvent.jsonForClient.description', '')
    PROC['desc']     = get_value(metadata, 'recMovie.jsonForClient.plot', PROC['desc'])
    PROC['title']    = get_value(metadata, 'recManualProgram.jsonForClient.title','')        
    PROC['title']    = get_value(metadata, 'recSportEvent.jsonForClient.eventTitle',PROC['title'])    
    PROC['title']    = get_value(metadata, 'recMovie.jsonForClient.title',PROC['title'])
    PROC['eid']      = get_value(metadata, 'recManualProgram.jsonForClient.objectID', '')
    PROC['eid']      = get_value(metadata, 'recSportEvent.jsonFromTribune.program.tmsId', clean(str(PROC['eid'])+'.'+str(PROC['title']), {' ':''}))
    PROC['eid']      = get_value(metadata, 'recMovieAiring.jsonFromTribune.program.tmsId', PROC['eid'])
    PROC['date']     = get_value(metadata, 'recManualProgramAiring.jsonForClient.airDate','')
    PROC['date']     = get_value(metadata, 'recSportEvent.jsonForClient.airDate',PROC['date'])
    PROC['date']     = get_value(metadata, 'recMovie.jsonForClient.releaseYear',PROC['date'])
    PROC['sid']      = get_value(metadata, 'recSeries.jsonFromTribune.tmsId', '')    
    PROC['series']   = get_value(metadata, 'recSeries.jsonForClient.title',PROC['title'])
    PROC['season']   = get_value(metadata, 'recEpisode.jsonForClient.seasonNumber','0')
    PROC['episode']  = get_value(metadata, 'recEpisode.jsonForClient.episodeNumber','0')
    PROC['title']    = get_value(metadata, 'recEpisode.jsonForClient.title',PROC['title'])
    PROC['desc']     = get_value(metadata, 'recEpisode.jsonForClient.description',PROC['desc'])
    PROC['airdate']  = get_value(metadata, 'recEpisode.jsonForClient.originalAirDate',PROC['airdate'])
    PROC['date']     = get_value(metadata, 'recEpisode.jsonForClient.airDate',PROC['date'])
    PROC['status']   = get_value(metadata, 'recEpisode.jsonForClient.video.state', PROC['status'])
    PROC['eid']      = get_value(metadata, 'recEpisode.jsonFromTribune.program.tmsId', PROC['eid'])
    PROC['quality']  = get_value(metadata, 'recEpisode.jsonForClient.video.height', PROC['quality'])
    PROC['type']     = get_value(metadata, 'recEpisode.jsonFromTribune.program.entityType', PROC['type'])
    PROC['genre']    = get_value(metadata, 'recEpisode.jsonFromTribune.program.genres', 'Drama')
    PROC['genre']    = get_value(metadata, 'recSportEvent.jsonFromTribune.program.genres', PROC['genre'])
    PROC['genre']    = get_value(metadata, 'recSeries.jsonFromTribune.genres', PROC['genre'])
    PROC['genre']    = get_value(metadata, 'recMovie.jsonFromTribune.genres', PROC['genre'])
    PROC['genre']    = get_value(metadata, 'recMovieAiring.jsonFromTribune.program.genres', PROC['genre'])
    PROC['director'] = get_value(metadata, 'recMovie.jsonForClient.directors', 'Unknown')
    PROC['director'] = get_value(metadata, 'recMovie.jsonFromTribune.directors', PROC['director'])
    PROC['tvse']     = ''

    PROC['genre'] = sList(PROC['genre'])

    MVMETADATA = {'\251nam': PROC['title'],  'title':PROC['title'],
                  '\251ART': PROC['director'], 'aART':PROC['director'], 'author':PROC['director'],
                  '\251gen': PROC['genre'], 'genre':PROC['genre'],
                  'cpil':'false', 'pgap':'false','hdvd':'2','stik':'Short Film','mediaType':'Short Film',
                  '\251day': PROC['end'], 'releaseDate':PROC['end'],
                  'desc':PROC['desc'], 'description':PROC['desc'],
                  'year':PROC['date']
                  }

    TVMETADATA = {'\251nam': PROC['title'],  'title':PROC['title'],
                  '\251ART': PROC['series'], 'aART':PROC['series'], '\251alb':PROC['series'], 'author':PROC['series'], 'album_artist':PROC['series'], 'tvsh':PROC['series'],
                  '\251gen': PROC['genre'], 'genre':PROC['genre'],
                  '\251day': PROC['end'], 'releaseDate':PROC['end'],
                  'desc':PROC['desc'], 'description':PROC['desc'],
                  'tvsn':PROC['season'], 'tvSeason':PROC['season'],
                  'soal':PROC['series']+', Season '+str(PROC['season']), 'sortAlbum':PROC['series']+', Season '+str(PROC['season']),
                  'stik':'TV Show', 'mediaType':'TV Show',
                  'hdvd':'2',
                  'cpil':'false', 'pgap':'false',
                  'tves':PROC['episode'],
                  'tven':PROC['episode'],
                  'episode_id':PROC['episode'], 'tvEpisode':PROC['episode'],
                  '\251ART': PROC['series'], 'aART':PROC['series'], 'tvsh':PROC['series'],
                  '\251alb':PROC['series'],
                  'author':PROC['series'], 'album_artist':PROC['series'],
                  'track':PROC['episode']
                  }

    if metadata.has_key('recSeason'):   # is a TV show!!!!
        PROC['type'] = 'tv'
        PROC['metadata'] = cleanmeta(TVMETADATA)
        if FLAGS.has_key('mce'):
            squished_date = "".join(string.splitfields(PROC['airdate'], '-'))
            PROC['tvse'] = 'S'+string.zfill(PROC['season'],2)+'E'+string.zfill(PROC['episode'],2)
            PROC['name'] = PROC['series'] + '_' + squished_date
        elif string.zfill(PROC['episode'],2) == "00":
            PROC['name'] = PROC['series'] + ' - ' + PROC['date'][:10]
            if PROC['title'] != '':
                PROC['name'] = PROC['name'] + ' - '+PROC['title']
        else:
            PROC['tvse'] = 'S'+string.zfill(PROC['season'],2)+'E'+string.zfill(PROC['episode'],2)
            PROC['name'] = PROC['series'] + ' - '+PROC['tvse']
            if PROC['title'] != '':
                PROC['name'] = PROC['name'] + ' - '+PROC['title']
    elif PROC['type'] == 'Sports':      # Is Sports!
        PROC['type'] = 'sports'
        PROC['metadata'] = cleanmeta(MVMETADATA)
        cleandate = string.splitfields(PROC['date'], 'T')[0]
        PROC['name'] = PROC['genre'] + ' - ' + PROC['title']+ ' (' +str(cleandate) + ')'
    elif PROC['type'] == 'manual':      # Manual Recording
        PROC['metadata'] = cleanmeta(MVMETADATA)
        PROC['type'] = 'movie'
        cleandate = string.splitfields(PROC['date'], 'T')[0]
        PROC['name'] = PROC['title']+ ' (' +str(cleandate) + ')'
    else:                               # is a Movie!!
        PROC['metadata'] = cleanmeta(MVMETADATA)
        PROC['type'] = 'movie'
        PROC['name'] = PROC['title']+ ' (' +str(PROC['date']) + ')'
    PROC['clean']    = clean(PROC['name'])

    PROC['end'] = timegm(time.strptime(PROC['end'].replace('Z', 'GMT'),'%Y-%m-%dT%H:%M%Z'))

    # allow for custom renaming
    if rDict(OPTIONS, [''], 'custom')[0] != '':
        PROC['name'] = custom(OPTIONS['custom'][0], PROC, convert_meta(metadata))
        PROC['clean'] = clean(PROC['name'])
        if PROC['tvse'] == '':
            PROC['tvse'] = 'S'+string.zfill(PROC['season'],2)+'E'+string.zfill(PROC['episode'],2)

    if rDict(OPTIONS, [''], 'customtv')[0] != '' and PROC['type'] == 'tv':
        PROC['name'] = custom(OPTIONS['customtv'][0], PROC, convert_meta(metadata))
        PROC['clean'] = clean(PROC['name'])
        if PROC['tvse'] == '':
            PROC['tvse'] = 'S'+string.zfill(PROC['season'],2)+'E'+string.zfill(PROC['episode'],2)
    elif rDict(OPTIONS, [''], 'custommovie')[0] != '' and PROC['type'] == 'movie':
        PROC['name'] = custom(OPTIONS['custommovie'][0], PROC, convert_meta(metadata))
        PROC['clean'] = clean(PROC['name'])
    elif rDict(OPTIONS, [''], 'customsports')[0] != '' and PROC['type'] == 'sports':
        PROC['name'] = custom(OPTIONS['customsports'][0], PROC, convert_meta(metadata))
        PROC['clean'] = clean(PROC['name'])
    
    return PROC

#################################################################################################
# show in most significant meta-data
def print_meta(DICT):
    clen = 0
    keys = DICT.keys()
    keys.sort()
    for key in keys:
        if len(key) > clen:
            clen = len(key)
    for key in keys:
        value = DICT[key]
        if str(type(value)) != "<type 'dict'>" and value != '':
            print('  '+string.ljust(key+':', clen+1)+' '+str(DICT[key]))

#################################################################################################
# show the list of matches
def print_match(MATCH, FLAGS):
    nMATCH = []
    for item in MATCH:
        temp = [rDict(item[2], 'UNKNOWN', 'clean'),rDict(item[2], 'UNKNOWN', 'eid'), item]
        nMATCH.append(temp)
    nMATCH.sort()
    for item in nMATCH:
        print(item[1]+' '+item[0])
        if FLAGS.has_key('long'):
            print_meta(item[2][2])
        if FLAGS.has_key('reallylong'):
            sDict(item[2][3])

#################################################################################################
# Function to download a single video file from the tablo
def get_file(IPADDR, VIDEOID, FILENUM, NEWFILE):
    try:
        cmd = 'http://'+IPADDR+':18080/pvr/'+str(VIDEOID)+'/segs/'+string.zfill(str(FILENUM),5)+'.ts'
        #debug(cmd)
        urllib.urlretrieve(cmd, NEWFILE)
    except KeyboardInterrupt:
        try:
            os.unlink(NEWFILE)
        except:
            ohwell = 1
        debug('Exiting during segment download')
        sys.exit()
    except:
        debug('Unable to download specific segment')

#################################################################################################
# Function to download and rebuild the videofile
# I no longer use ffmpeg just to concatenate video files, it was unnecessary
def get_video(IPADDR, VIDEOID, TMSID, FFMPEG, OPTIONS, FLAGS, show_proc):
    filename = None
    FAILED = False

    try:
        resp = urllib.urlopen('http://'+IPADDR+':18080/pvr/'+str(VIDEOID)+'/segs').read()
        final = string.splitfields(resp, '.ts')[:-1]
        final = string.splitfields(final[-1], '>')[-1]
        while(final[0]) == '0':
            final = final[1:]
        final = eval(final)
    except KeyboardInterrupt:
        debug('Exiting during segment count procedure')
        sys.exit()
    except:
        FAILED = True
        final = 0
        debug('Unable to retrieve segment information - Tablo Down')

    if not FAILED:
        debug('Video has '+str(final)+' segments', IPADDR)
        TEMPDIR = rDict(OPTIONS, ['./'], 'tempdir')[0]
        filename = TEMPDIR+'/'+TMSID+'.ts'
        skipable = False
        try:
            os.stat(filename)
        except:
            skipable = True
        if skipable and not FLAGS.has_key('skipdownload'):
            showcount = 15
            showsize = 0
            showstart = time.time()
            try:
                if not FLAGS.has_key('test'):
                    output = open(filename, 'wb')
                    for counter in range(1,final+1):
                        get_file(IPADDR, VIDEOID, counter, TEMPDIR+'/'+TMSID+str(counter)+'.ts')
                        filesize = os.stat(TEMPDIR+'/'+TMSID+str(counter)+'.ts')
                        filesize = int(filesize.st_size)
                        showsize = showsize + filesize
                        output.write(file(TEMPDIR+'/'+TMSID+str(counter)+'.ts', 'rb').read())
                        os.unlink(TEMPDIR+'/'+TMSID+str(counter)+'.ts')
                        if counter%showcount == 0 or counter==final or counter == 4:
                            shownowtime = int(((time.time() - showstart) / counter) * (final-counter))
                            showm = shownowtime / 60
                            shows = shownowtime % 60
                            debug(string.rjust(str(showsize/1000000),6)+' MB, '+string.rjust(str(int(100.0*counter/final)),3)+'%, '+str(showm)+'m '+str(shows)+'s',ipaddr)
                    output.close()
            except KeyboardInterrupt:
                debug('Exiting during video download')
                sys.exit()
            except SystemExit:
                debug('Removing .ts file')
                try:
                    output.close()
                except:
                    ohwell = 1
                try:
                    os.unlink(filename)
                except:
                    ohwell = 1
                sys.exit()
            except:
                debug('Unable to download video segments')
                filename = None
        
        if (FLAGS.has_key('mp4') or FLAGS.has_key('mkv') or FLAGS.has_key('cc')) and filename:
            debug('Converting video to .mp4',IPADDR)
            old = filename
            cc = TEMPDIR+'/'+TMSID+'.srt'
            if FLAGS.has_key('cc'):
                #ccextractor infile.ts -o infile.srt
                #ffmpeg -i infile.mp4 -f srt -i infile.srt -c:v copy -c:a copy -c:s mov_text outfile.mp4
                cmd = rDict(OPTIONS, ['./ccextractorwin.exe'], 'ccextract')[0] + ' -quiet ' + old + ' -o ' + cc
                cmd = string.split(cmd)
                debug('Extracting subtitles', IPADDR)
                if not FLAGS.has_key('test'):
                    subprocess.call(cmd)
            if FLAGS.has_key('mkv'):
                debug('Creating mkv file', IPADDR)
                filename = TEMPDIR+'/'+TMSID+'.mkv'
                cmd = rDict(OPTIONS, ['./HandBrakeCLI.exe'], 'handbrake')[0] + ' -i '+old+' -f -a 1 -E copy -f mkv -O -e x264 -q 22.0 --loose-anamorphic --modulus 2 -m --x264-preset medium --h264-profile high --h264-level 4.1 --decomb --denoise=weak -v -o '+filename
                cmd = string.split(cmd)
            else:
                debug('Creating mp4 file', IPADDR)
                filename = TEMPDIR+'/'+TMSID+'.mp4'
                if FLAGS.has_key('cc'):
                    cmd = [FFMPEG, '-y', '-loglevel','panic', '-i', old, '-f', 'srt', '-i', cc, '-bsf:a', 'aac_adtstoasc', '-c:v', 'copy', '-c:a','copy', '-c:s', 'mov_text', filename]
                else:
                    cmd = [FFMPEG, '-y', '-loglevel','panic', '-i', old, '-bsf:a', 'aac_adtstoasc', '-c', 'copy', filename]
            if not FLAGS.has_key('test'):
                subprocess.call(cmd)
                if tagging and FLAGS.has_key('mp4tag'):
                    keys = show_proc['metadata'].keys()
                    keys.sort()
                    mytags = MP4(filename)
                    for key in keys:
                        mytags[key] = show_proc['metadata'][key]
                    mytags.save()
                if not FLAGS.has_key('skipdelete'):
                    try:
                        debug('Deleteing .ts file', ipaddr)
                        os.unlink(old)
                    except:
                        debug('Unable to delete .ts file')
                    if FLAGS.has_key('cc'):
                        try:
                            debug('Deleteing .srt file', ipaddr)
                            os.unlink(cc)
                        except:
                            debug('Unable to delete .srt file')  

    return filename

#################################################################################################
# check for a match
def get_match(SEARCH_proc, SEARCH):
    results = False
    if SEARCH_proc.match(SEARCH) or SEARCH_proc.search(SEARCH):
        results = True
    return results

##########################################
###  Run the script (if not library)   ###
##########################################

#################################################################################################
if __name__ == "__main__":
    # Lets Begin
    
    # Parse all command line options
    OPTIONS, FLAGS = get_cmd_line(OPTIONS)
    LOGFILE = rDict(OPTIONS, ['tablo.log'], 'log')[0]
    if rDict(OPTIONS, [True], 'debug')[0] == True:
        debugging = 1
    else:
        debugging = 0

    debug('Starting TabloToGo Version '+str(VERSION))

    # Build Search Regex
    try:
        SEARCH_proc = re.compile(OPTIONS['{}'][0], re.IGNORECASE)
    except:
        print('FATAL: Invalid search specification')
        sys.exit()
            
    # Locate Tablos
    if OPTIONS['tablo'][0] == 'auto':   
        my_tablos = get_tablos()
        my_ips = my_tablos.keys()
        my_ips.sort()
    else:
        my_ips = OPTIONS['tablo'][0]
        if str(type(my_ips)) != "<type 'list'>":
            my_ips = [my_ips]
        my_tablos = {}
        for tablo in my_ips:
            my_tablos[tablo] = {'name':'*Manually Entered*'}

    # Initial CACHE Database
    if rDict(OPTIONS, ['ignore'], 'db')[0] != 'ignore':
        debug('Loading Cache Database')
    CDB = getDB(rDict(OPTIONS, ['ignoremeXXXX'], 'db')[0])
    DB = {}
    MATCH_DB = {}

    # Process tablo's to find new and interesting videos
    ProcessVideos = True
    while ProcessVideos:

        # Retrieve kmttg history file and tablo history file
        history = {}
        if not FLAGS.has_key('ignore'):
            debug('Loading download history')
            history = get_history(rDict(OPTIONS, ['auto.history'], 'kmttg')[0])
            history = get_history(rDict(OPTIONS, ['tablo.history'], 'history')[0], history)
            debug(str(len(history.keys()))+' shows and movies have already been downloaded')

        # Find new TV shows
        MATCH = []
        for ipaddr in my_ips:
            if not DB.has_key(ipaddr):
                DB[ipaddr] = {}
            if not CDB.has_key(ipaddr):
                CDB[ipaddr] = {}
            show_ids = get_list(ipaddr)
            debug('Found Tablo named ' + rDict(my_tablos[ipaddr], '', 'name'), ipaddr)
            debug('Loading meta-data for '+str(len(show_ids))+' recordings', ipaddr)
            new_recordings = 0
            selected_recordings = 0
            duplicates = 0
            new_tv = 0
            new_movie = 0
            new_sports = 0
            failed_meta = 0
            cache_count = 0
            show_ids.reverse() # Download newer copies of videos vice older ones (in case of dupes)
            for show_id in show_ids:
                if CDB[ipaddr].has_key(show_id):
                    DB[ipaddr][show_id] = proc_meta(CDB[ipaddr][show_id], OPTIONS)
                if CDB[ipaddr].has_key(show_id) \
                and DB[ipaddr].has_key(show_id) \
                and rDict(DB[ipaddr][show_id], 'X*X', 'status') == 'finished' \
                and rDict(DB[ipaddr][show_id], 0, 'cache') > time.time() - rDict(OPTIONS, [0], 'dbtime')[0]:
                    show_info = CDB[ipaddr][show_id]
                    cache_count = cache_count + 1
                else:
                    show_info = get_meta(ipaddr, show_id)
                if show_info == {}:
                    failed_meta = failed_meta + 1
                else:
                    show_proc = proc_meta(show_info, OPTIONS)
                    CDB[ipaddr][show_id] = show_info
                    DB[ipaddr][show_id] = show_proc                    
                    tmsid = rDict(show_proc, 'X*X', 'eid')
                    if not history.has_key(tmsid) and tmsid != 'X*X' and rDict(show_proc, 'X*X', 'status') == 'finished':
                        new_recordings = new_recordings + 1
                        correct_type = True
                        if rDict(show_proc, '', 'type') == 'movie':
                            new_movie = new_movie + 1
                            if FLAGS.has_key('tv') or FLAGS.has_key('sports'):
                                correct_type = False
                        elif rDict(show_proc, '', 'type') == 'tv':
                            new_tv = new_tv + 1
                            if FLAGS.has_key('movies') or FLAGS.has_key('sports'):
                                correct_type = False
                        elif rDict(show_proc, '', 'type') == 'sports':
                            new_sports = new_sports + 1
                            if FLAGS.has_key('tv') or FLAGS.has_key('movies'):
                                correct_type = False
                        show_time = (time.time() - rDict(show_proc, 0, 'end')) > int(rDict(OPTIONS, [0], 'delay')[0])
                        if show_time and correct_type and not MATCH_DB.has_key(tmsid) and (get_match(SEARCH_proc, rDict(show_proc, '', 'name')) or get_match(SEARCH_proc, rDict(show_proc, 'X*x*XX', 'genre')) or OPTIONS['{}'][0] == str(show_id) or OPTIONS['{}'][0] == str(tmsid)):
                            MATCH_DB[tmsid] = 1
                            if not FLAGS.has_key('not'):
                                selected_recordings = selected_recordings + 1
                                MATCH.append([ipaddr, show_id, show_proc, show_info])
                        elif MATCH_DB.has_key(tmsid):
                            duplicates = duplicates + 1
                        elif FLAGS.has_key('not') and not MATCH_DB.has_key(tmsid):
                            selected_recordings = selected_recordings + 1
                            MATCH.append([ipaddr, show_id, show_proc, show_info])
                            MATCH_DB[tmsid] = 1
                        
            if failed_meta != 0:
                debug('Failed to get metadata for '+str(failed_meta)+' videos', ipaddr)
            if cache_count > 0:
                debug(str(cache_count)+' entries were cached', ipaddr)
            if new_tv != 0 or new_movie != 0 or new_sports != 0:
                debug(str(new_tv)+' new TV, '+str(new_movie)+' movies, and '+str(new_sports)+' sports', ipaddr)
            if duplicates != 0:
                debug(str(duplicates)+' matching videos are duplicative', ipaddr)
            if selected_recordings != 0:
                debug(str(selected_recordings)+' matching videos have been queued', ipaddr)
            else:
                debug('No videos have been queued', ipaddr)
                
        if rDict(OPTIONS, ['ignore'], 'db')[0] != 'ignore':
            writeDB(rDict(OPTIONS, ['ignore'], 'db')[0], CDB)

        if not FLAGS.has_key('list') and not FLAGS.has_key('summary'):
            for item in MATCH:
                ipaddr = item[0]
                show_id = item[1]
                show_proc = item[2]
                filename = rDict(show_proc, 'x', 'clean')
                tmsid = rDict(show_proc, '', 'eid')
                if tmsid != '':
                    if not FLAGS.has_key('complete'):
                        # Download Video Files
                        debug('Downloading "'+filename+'"', ipaddr)
                        filename = get_video(ipaddr, show_id, tmsid, rDict(OPTIONS, ['ffmpeg.exe'], 'ffmpeg')[0], OPTIONS, FLAGS, show_proc)
  
                        if filename:
                            # Move and Rename Video File
                            if rDict(show_proc, '*', 'type') == 'tv' and rDict(show_proc, '', 'tvse') == '':                                
                                debug('Missing Season/Episode data - Move to fail dir', ipaddr)
                                if not FLAGS.has_key('test'):
                                    move_file(filename, rDict(OPTIONS, ['./'], 'faildir')[0], rDict(show_proc, 'tablo-fail', 'clean'), rDict(OPTIONS, ['./'], 'existsdir')[0])
                            if rDict(show_proc, '*', 'type') == 'movie':
                                debug('Moving to "'+rDict(OPTIONS, ['./'], 'moviedir')[0]+'"')
                                if not FLAGS.has_key('test'):
                                    move_file(filename, rDict(OPTIONS, ['./'], 'moviedir')[0], rDict(show_proc, 'tablo-fail', 'clean'), rDict(OPTIONS, ['./'], 'existsdir')[0])
                            if rDict(show_proc, '*', 'type') == 'sports':
                                debug('Moving to "'+rDict(OPTIONS, ['./'], 'sportsdir')[0]+'"')
                                if not FLAGS.has_key('test'):
                                    move_file(filename, rDict(OPTIONS, ['./'], 'sportsdir')[0], rDict(show_proc, 'tablo-fail', 'clean'), rDict(OPTIONS, ['./'], 'existsdir')[0])
                            if rDict(show_proc, '*', 'type') == 'tv' and rDict(show_proc, '', 'tvse') != '':
                                npath = rDict(OPTIONS, ['./'], 'tvdir')[0]+'/'+clean(rDict(show_proc, 'unknown', 'series'))+'/Season '+str(rDict(show_proc, 'unknown', 'season'))
                                if not FLAGS.has_key('tvcreate'):
                                    npath = rDict(OPTIONS, ['./'], 'tvdir')[0]
                                debug('Moving to "'+npath+'"', ipaddr)
                                if not FLAGS.has_key('test'):
                                    move_file(filename, npath, rDict(show_proc, 'tablo-fail', 'clean'), rDict(OPTIONS, ['./'], 'existsdir')[0])
                                            
                    # Mark file as downloaded
                    if (filename != None and filename != 'x') or FLAGS.has_key('complete'):
                        debug('Marking '+tmsid+' as processed', ipaddr)
                        if not FLAGS.has_key('test'):
                            write_history(rDict(OPTIONS, ['tablo.history'], 'history')[0], rDict(item[2], 'movie', 'type'), rDict(item[2], 'UNKNOWN', 'eid'), rDict(item[2], 'UNKNOWN', 'series'), rDict(item[2], 'UNKNOWN', 'title'), rDict(item[2], 'UNKNOWN', 'name'))

        # Should we delay and loop (in 'a'uto mode)
        if not FLAGS.has_key('a') or FLAGS.has_key('summary'):
            ProcessVideos = False
        else:
            try:
                debug('Sleeping for '+str(rDict(OPTIONS, [1800], 'sleep')[0])+' seconds')
                time.sleep(float(str(rDict(OPTIONS, [1800], 'sleep')[0])))
            except KeyboardInterrupt:
                ProcessVideos = False
                debug('Exiting')
 
    # Display a listing of matched videos
    if FLAGS.has_key('list') and not FLAGS.has_key('a'):
        print_match(MATCH, FLAGS)
    elif FLAGS.has_key('summary'):
        debug('Exiting via summary mode')

