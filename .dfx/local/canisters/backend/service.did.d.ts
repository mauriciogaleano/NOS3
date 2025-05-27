import type { Principal } from '@dfinity/principal';
import type { ActorMethod } from '@dfinity/agent';
import type { IDL } from '@dfinity/candid';

export interface HrvEntry {
  'stress' : string,
  'interaction' : bigint,
  'self' : bigint,
  'technology' : bigint,
  'surroundings' : bigint,
  'timestamp' : bigint,
  'place' : bigint,
  'rmssd' : bigint,
}
export interface User {
  'hrv' : Array<HrvEntry>,
  'principal' : Principal,
  'group' : string,
  'points' : bigint,
}
export interface _SERVICE {
  'addHrv' : ActorMethod<
    [bigint, bigint, bigint, bigint, bigint, bigint],
    boolean
  >,
  'feedback' : ActorMethod<[string], string>,
  'getOwner' : ActorMethod<[], Principal>,
  'getPoints' : ActorMethod<[], [] | [bigint]>,
  'getUser' : ActorMethod<[], [] | [User]>,
  'isOwner' : ActorMethod<[], boolean>,
  'register' : ActorMethod<[string], boolean>,
}
export declare const idlFactory: IDL.InterfaceFactory;
export declare const init: (args: { IDL: typeof IDL }) => IDL.Type[];
