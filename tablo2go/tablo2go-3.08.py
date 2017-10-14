#!/usr/bin/env python3

# TabloToGo version 3 - New Tablo APIs - J. Kenney 2016
# This uses the python requests library (pip install requests)

# Version 3 is a back-to-basics version due to new API
# Below are the editable options, stick with version 2
# If you require older options, Version 4 will be built
# to have a much nicer interface and capabilities, this
# is a proof of concept / test version.

                # 'series':disp_series,
                # 'season':disp_season,
                # 'episode':disp_episode,
                # 'title':disp_title,
                # 'year':disp_year,
                # 'duration':disp_duration,
                # 'height':disp_height,
                # 'description':disp_description,
                # 'date':disp_date,
                # 'network':disp_network,                   # Example: NBC
                # 'network_callsign':disp_network_callsign, # Example: WBAL-DT
                # 'channel':disp_channel                    # Example: 11.2

# Valid options, that can be placed in {} in NAME_SERIES and NAME_MOVIES are:
#   {series} {season} {episode} {title} {year} {date}
#   {network} {network_callsign} {channel}

# Note that any metadata apearing can be added manually to the code below, but
#   these are available by default.
NAME_SERIES = "/share/TiVo/{series}/{series} - S{season}E{episode} - {title}"
NAME_MOVIES = "/share/TiVo.Movies/{title} ({year})"

# Where is the ffmpeg executable located and what is the command that should be
# executed.  Use {build} for the filename without the extension, as {build} is
# the variable that will be built from the NAME_xxx above.
FFMPEG = '/kmttg/ffmpeg'
FFMPEG_CMD = ' -i "{m3u8}" -c copy -bsf:a aac_adtstoasc "{build}.mp4"'

# Set minimum duration and quality measurements
# Duration in seconds, Quality based on height (480, 720, 1080, etc.)
MIN_DURATION = 600
MIN_QUALITY = 100

# Set metadata and history files
FILE_TABLO_METADATA = '/kmttg/tablo3.mtd'
FILE_TABLO_HISTORY = '/kmttg/tablo3.history'
FILE_TIVO_HISTORY = '/kmttg/auto.history'

# Delete file from tablo after download (Not yet enabled!)
DELETE_FROM_TABLO = False

# Perform metadata update only, do not download videos
LIST_ONLY = False

# Default search specifications
# Types are movies, series, etc.
SEARCH = {'type':'',
          'id':'',
          'description':'',
          'series':'',
          'season':'',
          'episode':'',
          'title':'',
          'date':'',
          'year':'',
          'network':'',
          'network_callsign':'',
          'channel':''}

#############################################################
# Please do not edit below this line.
import os, sys

# return a value in a python style dictionary
def rDict(_dict, _default, *sequence):
    if not sequence:
        return _default
    for item in sequence:
        if not item in _dict:
            return _default
        _dict = _dict[item]
    if _dict is None:
        return _default
    return _dict

# print out a python dictionary, in an rDict compatible format
def pDict(_dict, loc=0, dpath=''):
    if type(_dict) is not dict:
        print(dpath+'-> '+str(_dict))
    else:
        for item in _dict:
            pDict(_dict[item], loc+1, dpath+"'"+str(item)+"', ")

# Clean a string of all bad characters
# Default ASCII ranges 48-57, 65-90, 97-122, - _ . are allowed,
#  otherwise BAD_CHARS can be defined directly
def clean(input, OVERRIDE={}):
    result = ''
    BAD_CHARS = {'(':'(', ')':')', ' ':' ', '"':' ', '&':'+', '/':' ', '\\':' ', '|':' ', "'":"", '?':'', u'\u2026':'', '@':'at ', u'\u2019':'',u'\xf8':''}
    for key in OVERRIDE:
        BAD_CHARS[key] = OVERRIDE[key]
    lastchar = 'Z*Z'
    for char in input:
        o = ord(char)
        if (o >= 48 and o <= 57) or (o >= 65 and o <= 90) or (o >= 97 and o <= 122) or (o == 95) or (o == 46) or (o == 45) or char in BAD_CHARS:
            if char in BAD_CHARS:
                char = BAD_CHARS[char]
            if char in BAD_CHARS:
                char = BAD_CHARS[char]
            if lastchar != ' ' or char != ' ':
                result = result + char
            lastchar = char
    return result

# Similiar to strip, will remove useless trailing characters
def squish(_input):
    _input = str(_input)
    _input = _input.strip()
    _input.replace('S00E00', '')
    while len(_input) > 0 and (_input[-1] == ' ' or _input[-1] == '-' or _input[-1] == '\n' or _input[-1] == '\r'):
        _input = _input[:-1]
    return _input

# Replace strings based on input from a dict
def fillin(_string, _dict):
    for key in _dict:
        try:
            _val = _dict[key].encode('ascii', 'ignore').decode()
        except:
            _val = str(_dict[key])
        _string = _string.replace('{'+str(key)+'}', str(_val))
    return _string

# Retrieve information from a REST style API
# Return the results in a python style dictionary
def get_api(HOST, PORT, API_PATH, HTTPS=False):
    import requests
    if HTTPS:
        CMD = 'https://' + HOST + ':' + str(PORT) + API_PATH
    else:
        CMD = 'http://' + HOST + ':' + str(PORT) + API_PATH
    resp = requests.get(CMD)
    if resp.status_code == 200:
        return resp.json()
    else:
        return {}

# Retrieve information from a REST style API
# Return the results in a python style dictionary
def post_api(HOST, PORT, API_PATH, HTTPS=False):
    import requests
    if HTTPS:
        CMD = 'https://' + HOST + ':' + str(PORT) + API_PATH
    else:
        CMD = 'http://' + HOST + ':' + str(PORT) + API_PATH
    resp = requests.post(CMD)
    if resp.status_code == 200:
        return resp.json()
    else:
        return {}

# Retrieve a stored
def load_metadata(filename='tablo3.mtd'):
    try:
        results = open(filename,'r').readlines()
        results = eval(results[0])
        return results
    except:
        return {}

# Save metadata
def save_metadata(METADATA, filename='tablo3.mtd'):
    try:
        results = open(filename, 'w')
        results.write(str(METADATA)+'\n')
        results.close()
    except:
        pass

# Load History
def load_history(filename, results={}):
    try:
        hdata = open(filename, 'r').readlines()
        for line in hdata:
            line = line.strip()
            if line.find(' ') != -1:
                tmp = line.split()
                results[tmp[0]] = line
    except:
        pass
    return results

# Update History
def update_history(filename, tmsid, desc):
    hdata = open(filename, 'a')
    hdata.write(tmsid+' '+desc+'\n')
    hdata.close()

# Load metadata if stored
HISTORY = load_history(FILE_TABLO_HISTORY)
HISTORY = load_history(FILE_TIVO_HISTORY, HISTORY)
METADATA = load_metadata(FILE_TABLO_METADATA)
METADATA_UPDATED = False
METADATA_VALID = {}
QUEUE = {}

# Acquire a list of available tablo's
found_tablos = get_api('api.tablotv.com','443','/assocserver/getipinfo/', HTTPS=True)
found_tablos = rDict(found_tablos, [], 'cpes')

# Download Metadata from found tablos
for tablo in found_tablos:
    TABLO_IP = tablo['private_ip']
    QUEUE[TABLO_IP] = {}
    METADATA_VALID[TABLO_IP] = {'series':{}, 'airing':{}, 'season':{}}
    if TABLO_IP not in METADATA:
        METADATA[TABLO_IP] = {}
    if 'series' not in METADATA[TABLO_IP]:
        METADATA[TABLO_IP]['series'] = {}
    if 'airing' not in METADATA[TABLO_IP]:
        METADATA[TABLO_IP]['airing'] = {}
    if 'season' not in METADATA[TABLO_IP]:
        METADATA[TABLO_IP]['season'] = {}
    METADATA[TABLO_IP]['api.tablotv.com'] = tablo
    METADATA[TABLO_IP]['info'] = get_api(TABLO_IP, '8885', '/server/info')
    if sys.argv[-1] == 'quiet':
        quietmode = True
    else:
        print(TABLO_IP + ' - Found Tablo - ' + rDict(tablo, 'unk-name', 'name') + ' ('+rDict(tablo, 'unk-type', 'board_type')+')')
    series_list = get_api(TABLO_IP, '8885', '/recordings/shows')
    airings_list = get_api(TABLO_IP, '8885', '/recordings/airings')
    for series in series_list:
        series_num = series.split('/')[-1]
        METADATA_VALID[TABLO_IP]['series'][series_num] = 1
        if series_num not in METADATA[TABLO_IP]['series']:
            METADATA_UPDATED = True
            METADATA[TABLO_IP]['series'][series_num] = get_api(TABLO_IP, '8885', series)
            print(TABLO_IP + ' - Loaded Series Metadata for ' + rDict(METADATA[TABLO_IP]['series'][series_num], 'unk-title', 'series', 'title'))
    for airing in airings_list:
        airing_type = airing.split('/')[2]
        airing_num = airing.split('/')[-1]
        airing_show = False
        METADATA_VALID[TABLO_IP]['airing'][airing_num] = 1
        if airing_num not in METADATA[TABLO_IP]['airing']:
            METADATA[TABLO_IP]['airing'][airing_num] = get_api(TABLO_IP, '8885', airing)
            airing_show = True
            METADATA_UPDATED = True
        season = rDict(METADATA[TABLO_IP]['airing'][airing_num], '/unk-season', 'season_path')
        season_num = season.split('/')[-1]
        METADATA_VALID[TABLO_IP]['season'][season_num] = 1
        if season_num not in METADATA[TABLO_IP]['season']:
            METADATA[TABLO_IP]['season'][season_num] = get_api(TABLO_IP, '8885', season)
            METADATA_UPDATED = True
        METADATA[TABLO_IP]['airing'][airing_num]['season_info'] = METADATA[TABLO_IP]['season'][season_num]
        airing_series = rDict(METADATA[TABLO_IP]['airing'][airing_num], '', 'series_path')
        airing_series = airing_series.split('/')[-1]
        METADATA[TABLO_IP]['airing'][airing_num]['series_info'] = rDict(METADATA[TABLO_IP]['series'], {}, airing_series)
        if airing_type == 'movies':
            METADATA[TABLO_IP]['airing'][airing_num]['movie_info'] = get_api(TABLO_IP, '8885', rDict(METADATA[TABLO_IP]['airing'][airing_num], 'unk-movie', 'movie_path'))
        disp_series = rDict(METADATA[TABLO_IP]['airing'][airing_num], '', 'series_info', 'series', 'title')
        disp_season = rDict(METADATA[TABLO_IP]['airing'][airing_num], '', 'season_info', 'season', 'number')
        disp_season = str(disp_season).zfill(2)
        disp_episode = rDict(METADATA[TABLO_IP]['airing'][airing_num], '', 'episode', 'number')
        disp_episode = str(disp_episode).zfill(2)
        disp_title = rDict(METADATA[TABLO_IP]['airing'][airing_num], '', 'episode', 'title')
        disp_title = rDict(METADATA[TABLO_IP]['airing'][airing_num], disp_title, 'movie_info', 'movie', 'title')
        disp_year = rDict(METADATA[TABLO_IP]['airing'][airing_num], '', 'movie_info', 'movie', 'release_year')
        disp_duration = rDict(METADATA[TABLO_IP]['airing'][airing_num], 0, 'video_details', 'duration')
        disp_height = rDict(METADATA[TABLO_IP]['airing'][airing_num], 0, 'video_details', 'height')
        disp_state = rDict(METADATA[TABLO_IP]['airing'][airing_num], '', 'video_details', 'state')
        disp_description = rDict(METADATA[TABLO_IP]['airing'][airing_num], '', 'episode', 'description')
        disp_description = rDict(METADATA[TABLO_IP]['airing'][airing_num], disp_description, 'movie_info', 'movie', 'plot')
        disp_date = rDict(METADATA[TABLO_IP]['airing'][airing_num], '', 'airing_details', 'datetime')
        disp_id = rDict(METADATA[TABLO_IP]['airing'][airing_num], '', 'episode', 'tms_id')
        disp_id = rDict(METADATA[TABLO_IP]['airing'][airing_num], disp_id, 'movie_airing', 'tms_id')
        disp_network = rDict(METADATA[TABLO_IP]['airing'][airing_num], '', 'channel', 'network')
        disp_network_callsign = rDict(METADATA[TABLO_IP]['airing'][airing_num], '', 'channel', 'call_sign')
        disp_channel_major = rDict(METADATA[TABLO_IP]['airing'][airing_num], '', 'channel', 'major')
        disp_channel_minor = rDict(METADATA[TABLO_IP]['airing'][airing_num], '', 'channel', 'minor')
        disp_channel = disp_channel_major+'.'+disp_channel_minor
        if disp_state == 'finished':        # options are finished, recording, and failed
            QUEUE[TABLO_IP][airing_num] = {
                'series':disp_series,
                'season':disp_season,
                'episode':disp_episode,
                'title':disp_title,
                'year':disp_year,
                'duration':disp_duration,
                'height':disp_height,
                'description':disp_description,
                'date':disp_date,
                'type':airing_type,
                'network':disp_network,
                'network_callsign':disp_network_callsign,
                'channel':disp_channel}
            if airing_type == 'movies':
                disp_build = squish(fillin(NAME_MOVIES, QUEUE[TABLO_IP][airing_num]))
                if disp_id == '':
                    disp_id = clean(disp_build, {' ':'.'})
            elif airing_type == 'series':
                disp_build = squish(fillin(NAME_SERIES, QUEUE[TABLO_IP][airing_num]))
                if disp_id == '' or disp_id.find('SH') == 0:
                    disp_id = clean(disp_build, {' ':'.'})
            if disp_state == 'finished':
                QUEUE[TABLO_IP][airing_num]['id'] = disp_id
                QUEUE[TABLO_IP][airing_num]['build'] = disp_build
            if airing_show:
                print(TABLO_IP+' - Loaded Metadata - '+disp_build)

# Determine if there is any unnecessary metadata that should be removed
METADATA_DELETE = []
for TABLO_IP in METADATA:
    for mode in ('series', 'airing', 'season'):
        for item in METADATA[TABLO_IP][mode]:
            if rDict(METADATA_VALID, 'delete', TABLO_IP, mode, item) == 'delete':
                METADATA_DELETE.append([TABLO_IP, mode, item])
                METADATA_UPDATED = True

# Delete unnecessary metadata
for line in METADATA_DELETE:
    del(METADATA[line[0]][line[1]][line[2]])

# save metadata to file if there are any changes
if METADATA_UPDATED:
    save_metadata(METADATA, FILE_TABLO_METADATA)

# Go through queued items
for TABLO_IP in QUEUE:
    # Sort by date, most recent first
    SORTER = []
    for airing_num in QUEUE[TABLO_IP]:
        SORTER.append([QUEUE[TABLO_IP][airing_num]['date']+airing_num, QUEUE[TABLO_IP][airing_num]['date'], airing_num])
    SORTER.sort()
    for airing_num_data in SORTER:
        dtg = airing_num_data[1]
        dtg = dtg.replace('T',' ')
        airing_num = airing_num_data[2]
        height = QUEUE[TABLO_IP][airing_num]['height']
        duration = QUEUE[TABLO_IP][airing_num]['duration']
        history_id = QUEUE[TABLO_IP][airing_num]['id']
        match_failed = False;
        for search_criteria in SEARCH:
            if SEARCH[search_criteria] != '':
                if search_criteria in QUEUE[TABLO_IP][airing_num]:
                    if str(QUEUE[TABLO_IP][airing_num][search_criteria]).lower().find(str(SEARCH[search_criteria].lower())) == -1:
                        match_failed = True;
        if duration >= MIN_DURATION and height >= MIN_QUALITY and not match_failed and history_id not in HISTORY:
            playlist = {}
            if QUEUE[TABLO_IP][airing_num]['type'] == 'series':
                playlist = post_api(TABLO_IP, '8885', '/recordings/series/episodes/'+str(airing_num)+'/watch')
            elif QUEUE[TABLO_IP][airing_num]['type'] == 'movies':
                playlist = post_api(TABLO_IP, '8885', '/recordings/movies/airings/'+str(airing_num)+'/watch')
            QUEUE[TABLO_IP][airing_num]['m3u8'] = rDict(playlist, '', 'playlist_url')
            run_command = FFMPEG + ' ' + FFMPEG_CMD
            run_command = fillin(run_command, QUEUE[TABLO_IP][airing_num])
            print()
            print(TABLO_IP+' - '+ airing_num + ' - '+ dtg + ' - ' +history_id+' - Downloading - ' + QUEUE[TABLO_IP][airing_num]['build'])
            if os.path.isfile(QUEUE[TABLO_IP][airing_num]['build']+'.mp4'):
                print(TABLO_IP+' - Download Aborted File Already Exists, Marking as Complete')
                update_history(FILE_TABLO_HISTORY,history_id, QUEUE[TABLO_IP][airing_num]['build']+" * Saved already by Tablo")
            elif os.path.isfile(QUEUE[TABLO_IP][airing_num]['build']+'.mkv'):
                print(TABLO_IP+' - Download Aborted File Provided By TiVo, Marking as Complete')
                update_history(FILE_TABLO_HISTORY,history_id, QUEUE[TABLO_IP][airing_num]['build']+" * Saved already by TiVo")
            elif not LIST_ONLY:
                try:
                    os.makedirs(os.path.dirname(QUEUE[TABLO_IP][airing_num]['build'])) # , exist_ok=True)
                except:
                    pass
                os.system(run_command)
                if os.path.isfile(QUEUE[TABLO_IP][airing_num]['build']+'.mp4'):
                    print(TABLO_IP+' - Updating History file - '+str(QUEUE[TABLO_IP][airing_num]['build'])+'.mp4')
                    update_history(FILE_TABLO_HISTORY,history_id, QUEUE[TABLO_IP][airing_num]['build'])
        elif sys.argv[-1] == 'quiet':
            quietmode = True
        elif duration < MIN_DURATION:
            print(TABLO_IP+' - '+ airing_num + ' - ' + dtg + ' - '  + history_id+' - Video Length ('+str(duration)+') to Short! - ' + QUEUE[TABLO_IP][airing_num]['build'])
        elif height < MIN_QUALITY:
            print(TABLO_IP+' - '+ airing_num + ' - ' + dtg + ' - '  + history_id+' - Video Quality ('+str(height)+') to Low! - ' + QUEUE[TABLO_IP][airing_num]['build'])
        elif match_failed:
            print(TABLO_IP+' - '+ airing_num + ' - ' + dtg + ' - '  + history_id+' - Failed Match Criteria - ' + QUEUE[TABLO_IP][airing_num]['build'])
        elif history_id in HISTORY:
            print(TABLO_IP+' - '+ airing_num + ' - ' + dtg + ' - '  + history_id+' - Already Downloaded ' + QUEUE[TABLO_IP][airing_num]['build'])
