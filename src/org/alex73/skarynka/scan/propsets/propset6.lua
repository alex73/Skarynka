--[[
GENERATED PROPCASE TABLE
use propcase.lua instead
--]]
return {
  AE_LOCK=3,          -- 0 = AE not locked, 1 = AE locked
  AF_ASSIST_BEAM=5,          -- 0=disabled,  1=enabled
  REAL_FOCUS_MODE=6,          --??? WIKI|Propcase focus_mode
  AF_FRAME=8,          -- 1 = FlexiZone, 2 = Face AiAF / Tracking AF
  AF_LOCK=11,         -- 0 = AF not locked, 1 = AF locked
  CONTINUOUS_AF=12,         -- 0 = Continuous AF off, 1 = Continuous AF on
  FOCUS_STATE=18,         --???
  AV2=22,         -- (philmoz, May 2011) - this value causes overrides to be saved in JPEG and shown on Canon OSD
  AV=23,         -- This values causes the actual aperture value to be overriden
  MIN_AV=25,
  USER_AV=26,
  BRACKET_MODE=29,
  BV=34,
  SHOOTING_MODE=49,
  CUSTOM_SATURATION=55,         -- Canon Menu slide bar values: 255, 254, 0, 1, 2
  QUALITY=57,
  CUSTOM_CONTRAST=59,         -- Canon Menu slide bar values: 255, 254, 0, 1, 2
  FLASH_SYNC_CURTAIN=64,
  SUBJECT_DIST2=65,
  DATE_STAMP=66,         -- 0 = Off, 1 = Date, 2 = Date & Time
  DELTA_SV=79,
  DIGITAL_ZOOM_MODE=91,         -- Digital Zoom Mode/State 0 = off/standard, 2 = 1.5x, 3 = 2.0x
  DIGITAL_ZOOM_STATE=94,         -- Digital Zoom Mode/State 0 = Digital Zoom off, 1 = Digital Zoom on
  DIGITAL_ZOOM_POSITION=95,
  DRIVE_MODE=102,
  OVEREXPOSURE=103,
  DISPLAY_MODE=105,
  EV_CORRECTION_1=107,
  FLASH_ADJUST_MODE=121,
  FLASH_FIRE=122,
  FLASH_EXP_COMP=127,    -- APEX96 units
  FOCUS_MODE=133,
  FLASH_MANUAL_OUTPUT=141,        -- !not sure, but required for compile; from propset4
  FLASH_MODE=143,        -- 0 = Auto, 1 = ON, 2 = OFF
  IS_MODE=145,        -- 0 = Continuous, 2 = only Shoot, 4 = OFF
  ISO_MODE=149,
  METERING_MODE=157,        -- 0 = Evaluative, 1 = Spot, 2 = Center weighted avg
  CUSTOM_BLUE=176,        -- Canon Menu slide bar values: 255, 254, 0, 1, 2
  CUSTOM_GREEN=177,        -- Canon Menu slide bar values: 255, 254, 0, 1, 2
  CUSTOM_RED=178,        -- Canon Menu slide bar values: 255, 254, 0, 1, 2
  CUSTOM_SKIN_TONE=179,        -- Canon Menu slide bar values: 255, 254, 0, 1, 2
  MY_COLORS=187,        -- 0 = Off, 1 = Vivid, 2 = Neutral, 3 = B/W, 4 = Sepia, 5 = Positive Film, 6 = Lighter Skin Tone, 7 = Darker Skin Tone, 8 = Vivid Red, 9 = Vivid Green, 10 = Vivid Blue, 11 = Custom Color
  ND_FILTER_STATE=195,        -- 0 = out, 1 = in
  OPTICAL_ZOOM_POSITION=198, 
  EXPOSURE_LOCK=209,     -- Old PROPCASE_SHOOTING value - gets set when set_aelock called or AEL button pressed
  SHOOTING=302,     -- This value appears to work better - gets set to 1 when camera has focused and set exposure, returns to 0 after shot
  EV_CORRECTION_2=210,
  IS_FLASH_READY=211,
  RESOLUTION=221,        -- 0 = L, 1 = M1, 2 = M2, 4 = S, 7 = Low Light
  ORIENTATION_SENSOR=222,
  TIMER_MODE=226,        -- 0 = OFF, 1 = 2 sec, 2 = 10 sec, 3 = Custom
  TIMER_DELAY=227,        -- timer delay in msec
  CUSTOM_SHARPNESS=228,        -- Canon Menu slide bar values: 255, 254, 0, 1, 2
  STITCH_DIRECTION=236,        -- 0=left>right, 1=right>left. Some cams have more
  STITCH_SEQUENCE=241,        -- counts shots in stitch sequence, positive=left>right, negative=right>left
  SUBJECT_DIST1=248,
  SV_MARKET=249,
  TV2=264,        -- (philmoz, May 2011) - this value causes overrides to be saved in JPEG and shown on Canon OSD
  TV=265,        -- Need to set this value for overrides to work correctly
  USER_TV=267,
  WB_MODE=271,        -- 0 = Auto, 1 = Daylight, 2 = Cloudy, 3 = Tungsten, 4 = Fluorescent, 5 = Fluorescent H, 7 = Custom
  WB_ADJ=272,
  SERVO_AF=298,        -- 0 = Servo AF off, 1 = Servo AF on
  ASPECT_RATIO=299,        -- 0 = 4:3, 1 = 16:9, 2 = 3:2, 3 = 1:1
  SV=346,        -- (philmoz, May 2011) - this value causes overrides to be saved in JPEG and shown on Canon OSD
  GPS=357,        -- (CHDKLover, August 2011) - contains a 272 bytes long structure
  TIMER_SHOTS=376,        -- Number of shots for TIMER_MODE=Custom
}
