function [PLd , APDd , MPDd , PLv , APDv , MPDv, TT] = Simulator3A(lambda,C,f,P, n, b)
% INPUT PARAMETERS:
%  lambda - packet rate (packets/sec)
%  C      - link bandwidth (Mbps)
%  f      - queue size (Bytes)
%  P      - number of packets (stopping criterium)
%  b      - bit error 
% OUTPUT PARAMETERS:
%  PL   - packet loss (%)
%  APD  - average packet delay (milliseconds)
%  MPD  - maximum packet delay (milliseconds)
%  TT   - transmitted throughput (Mbps)

%Events:
ARRIVAL= 0;       % Arrival of a packet            
DEPARTURE= 1;     % Departure of a packet

% Packet type:
DATA = 0;
VoIP = 1;

%State variables:
STATE = 0;          % 0 - connection is free; 1 - connection is occupied
QUEUEOCCUPATION= 0; % Occupation of the queue (in Bytes)
QUEUE= [];          % Size and arriving time instant of each packet in the queue

%Statistical Counters:
TOTALPACKETSd= 0;     % No. of packets arrived to the system
LOSTPACKETSd= 0;      % No. of packets dropped due to buffer overflow
TRANSPACKETSd= 0;     % No. of transmitted packets
TRANSBYTESd= 0;       % Sum of the Bytes of transmitted packets
DELAYSd= 0;           % Sum of the delays of transmitted packets
MAXDELAYd= 0;         % Maximum delay among all transmitted packets

TOTALPACKETSv= 0;     % No. of packets arrived to the system
LOSTPACKETSv= 0;      % No. of packets dropped due to buffer overflow
TRANSPACKETSv= 0;     % No. of transmitted packets
TRANSBYTESv= 0;       % Sum of the Bytes of transmitted packets
DELAYSv= 0;           % Sum of the delays of transmitted packets
MAXDELAYv= 0;         % Maximum delay among all transmitted packets

% Initializing the simulation clock:
Clock= 0;

% Initializing the List of Events with the first ARRIVAL of DATA packets:
tmp= Clock + exprnd(1/lambda);
Event_List = [ARRIVAL, tmp, GenerateDataPacketSize(), tmp, DATA];

% Initializing the List of Events with the first ARRIVAL OF VoIP Packets
for i = 1:n
    tmp = unifrnd(0, 0.02);
    Event_List = [Event_List; ARRIVAL, tmp, randi([110, 130]), tmp, VoIP];
end

%Similation loop:
while (TRANSPACKETSd + TRANSPACKETSv) <P                     % Stopping criterium
    Event_List= sortrows(Event_List,2);  % Order EventList by time
    Event= Event_List(1,1);                 % Get first event 
    Clock= Event_List(1,2);                 %    and all
    PacketSize= Event_List(1,3);            %    associated
    ArrInstant= Event_List(1,4);            %    parameters.
    PacketType = Event_List(1,5);
    Event_List(1,:)= [];                 % Eliminate first event
    switch Event
        case ARRIVAL         % If first event is an ARRIVAL
            if (PacketType == DATA)
                TOTALPACKETSd= TOTALPACKETSd+1;
                tmp= Clock + exprnd(1/lambda);
                Event_List = [Event_List; ARRIVAL, tmp, GenerateDataPacketSize(), tmp, DATA];
                if STATE==0
                    STATE= 1;
                    Event_List = [Event_List; DEPARTURE, Clock + 8*PacketSize/(C*1e6), PacketSize, Clock, DATA];
                else
                    if QUEUEOCCUPATION + PacketSize <= f
                        QUEUE= [QUEUE;PacketSize , Clock, DATA];
                        QUEUEOCCUPATION= QUEUEOCCUPATION + PacketSize;
                    else
                        LOSTPACKETSd= LOSTPACKETSd + 1;
                    end
                end
            else
                TOTALPACKETSv= TOTALPACKETSv+1;
                tmp= Clock + unifrnd(0.016, 0.024);
                Event_List = [Event_List; ARRIVAL, tmp, randi([110, 130]), tmp, VoIP];
                if STATE==0
                    STATE= 1;
                    Event_List = [Event_List; DEPARTURE, Clock + 8*PacketSize/(C*1e6), PacketSize, Clock, VoIP];
                else
                    if QUEUEOCCUPATION + PacketSize <= f
                        QUEUE= [QUEUE;PacketSize , Clock, VoIP];
                        QUEUEOCCUPATION= QUEUEOCCUPATION + PacketSize;
                    else
                        LOSTPACKETSv= LOSTPACKETSv + 1;
                    end
                end

            end

        case DEPARTURE          % If first event is a DEPARTURE
            if(rand() < (1-b)^(PacketSize *8))          % chegar sem erros
                if (PacketType == DATA)
                    TRANSBYTESd= TRANSBYTESd + PacketSize;
                    DELAYSd= DELAYSd + (Clock - ArrInstant);
                    if Clock - ArrInstant > MAXDELAYd
                        MAXDELAYd= Clock - ArrInstant;
                    end
                    TRANSPACKETSd= TRANSPACKETSd + 1;
                else
                   TRANSBYTESv= TRANSBYTESv + PacketSize;
                   DELAYSv= DELAYSv + (Clock - ArrInstant);
                   if Clock - ArrInstant > MAXDELAYv
                       MAXDELAYv= Clock - ArrInstant;
                   end
                   TRANSPACKETSv= TRANSPACKETSv + 1; 
                end
            else                                    % chegar com erros
                if (PacketType == DATA)
                    LOSTPACKETSd = LOSTPACKETSd + 1;
                else
                    LOSTPACKETSv = LOSTPACKETSv + 1;
                end
            end

            if QUEUEOCCUPATION > 0
                QSize= QUEUE(1,1);
                QInstant= QUEUE(1,2);
                Qso = QUEUE(1,3);
                Event_List = [Event_List; DEPARTURE, Clock + 8*QSize/(C*1e6), QSize, QInstant, Qso];
                QUEUEOCCUPATION= QUEUEOCCUPATION - QSize;
                QUEUE(1,:)= [];
            else
                STATE= 0;
            end
    end
end

%Performance parameters determination:
PLd= 100*LOSTPACKETSd/TOTALPACKETSd;  % in percentage
APDd= 1000*DELAYSd/TRANSPACKETSd;     % in milliseconds
MPDd= 1000*MAXDELAYd;                % in milliseconds

PLv= 100*LOSTPACKETSv/TOTALPACKETSv;  % in percentage
APDv= 1000*DELAYSv/TRANSPACKETSv;     % in milliseconds
MPDv= 1000*MAXDELAYv;                % in milliseconds

TT= 1e-6*(TRANSBYTESd + TRANSBYTESv)*8/Clock;       % in Mbps

end

function out= GenerateDataPacketSize()
    aux= rand();
    aux2= [65:109 111:1517];
    if aux <= 0.19
        out= 64;
    elseif aux <= 0.19 + 0.23
        out= 110;
    elseif aux <= 0.19 + 0.23 + 0.17
        out= 1518;
    else
        out = aux2(randi(length(aux2)));
    end
end