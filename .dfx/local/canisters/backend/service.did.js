export const idlFactory = ({ IDL }) => {
  const HrvEntry = IDL.Record({
    'stress' : IDL.Text,
    'interaction' : IDL.Nat,
    'self' : IDL.Nat,
    'technology' : IDL.Nat,
    'surroundings' : IDL.Nat,
    'timestamp' : IDL.Int,
    'place' : IDL.Nat,
    'rmssd' : IDL.Nat,
  });
  const User = IDL.Record({
    'hrv' : IDL.Vec(HrvEntry),
    'principal' : IDL.Principal,
    'group' : IDL.Text,
    'points' : IDL.Nat,
  });
  return IDL.Service({
    'addHrv' : IDL.Func(
        [IDL.Nat, IDL.Nat, IDL.Nat, IDL.Nat, IDL.Nat, IDL.Nat],
        [IDL.Bool],
        [],
      ),
    'feedback' : IDL.Func([IDL.Text], [IDL.Text], ['query']),
    'getOwner' : IDL.Func([], [IDL.Principal], ['query']),
    'getPoints' : IDL.Func([], [IDL.Opt(IDL.Nat)], ['query']),
    'getUser' : IDL.Func([], [IDL.Opt(User)], ['query']),
    'isOwner' : IDL.Func([], [IDL.Bool], []),
    'register' : IDL.Func([IDL.Text], [IDL.Bool], []),
  });
};
export const init = ({ IDL }) => { return []; };
